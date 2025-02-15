package org.example.service;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.example.repository.UserRepository;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class Bot extends TelegramLongPollingBot{
    //Статусная модель процесса формирования документа
    private static final String STATE_WAITING_FOR_FIO = "WAITING_FOR_FIO";
    private static final String STATE_WAITING_FOR_BIRTHDATE = "WAITING_FOR_BIRTHDATE";
    private static final String STATE_WAITING_FOR_GENDER = "WAITING_FOR_GENDER";
    private static final String STATE_WAITING_FOR_PHOTO = "WAITING_FOR_PHOTO";

    private Map<Long, String> userStates = new HashMap<>(); // Временное хранение состояний пользователей
    private Map<String, String> userData = new HashMap<>();   // Временное хранение введенных данных пользователей

    /**
     * Функция обеспечивает основную логику обработки событий
     * @param update входящее событие Telegram
     * @return void
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            String chatId = update.getMessage().getChatId().toString();
            long userId = update.getMessage().getChatId();

            if (update.getMessage().hasText()) {
                String text = update.getMessage().getText();

                if (text.equals("/start")) {
                    checkUser(userId, chatId, update);
                    sendWelcomeMessage(chatId);
                } else if (userStates.containsKey(userId)) {
                    String state = userStates.get(userId);

                    if (state.equals(STATE_WAITING_FOR_FIO)) {
                        // Проверяем введенные ФИО
                        if (validateFIO(text)) {
                            userData.put(userId + "_fio", text); // Сохраняем ФИО
                            sendMessage(chatId, "ФИО принято. Теперь введите вашу дату рождения в формате dd.MM.yyyy (например, 31.12.1990):");
                            userStates.put(userId, STATE_WAITING_FOR_BIRTHDATE); // Переходим к состоянию ввода даты рождения
                        } else {
                            sendMessage(chatId, "Ошибка! Введите ФИО еще раз (например, Иванов Иван Иванович):");
                        }
                    } else if (state.equals(STATE_WAITING_FOR_BIRTHDATE)) {
                        // Проверяем введенную дату рождения
                        if (validateBirthdate(text)) {
                            userData.put(userId + "_birthdate", text); // Сохраняем дату рождения
                            sendGenderSelection(chatId); // Запрашиваем пол
                            userStates.put(userId, STATE_WAITING_FOR_GENDER); // Переходим к состоянию выбора пола
                        } else {
                            sendMessage(chatId, "Ошибка! Введите дату рождения еще раз в формате dd.MM.yyyy (например, 31.12.1990). Убедитесь, что дата не позже текущего дня.");
                        }
                    }
                }
            } else if (update.getMessage().hasPhoto()) {
                // Обработка загрузки фотографии
                if (userStates.containsKey(userId) && userStates.get(userId).equals(STATE_WAITING_FOR_PHOTO)) {
                    // Получаем фотографию
                    List<PhotoSize> photos = update.getMessage().getPhoto();
                    PhotoSize photo = photos.get(photos.size() - 1); // Берем фото с самым высоким разрешением
                    String fileId = photo.getFileId();

                    // Сохраняем fileId фотографии
                    userData.put(userId + "photo", fileId);

                    // Формируем документ
                    try {
                        String fio = userData.get(userId + "_fio");
                        String birthdate = userData.get(userId + "_birthdate");
                        String gender = userData.get(userId + "_gender");

                        createUserDocument(userId, fio, birthdate, gender, userData.get(userId + "photo"));

                        // Отправляем документ пользователю
                        String fileName = "user_" + userId + ".docx";
                        java.io.File documentFile = new java.io.File(fileName);
                        SendDocument sendDocument = new SendDocument(chatId, new InputFile(documentFile));
                        execute(sendDocument);

                        sendMessage(chatId, "Ваши данные представлены в документе.");
                        deleteUserData(userId);

                    } catch (IOException e) {
                        e.printStackTrace();
                        sendMessage(chatId, "Ошибка при создании документа. Попробуйте еще раз.");
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    } catch (InvalidFormatException e) {
                        throw new RuntimeException(e);
                    }

                    userStates.remove(userId); // Завершаем состояние
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            long userId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals("agree")) {
                UserRepository.updatePolicyAcceptance(userId, true);
                sendMessage(chatId, "Пожалуйста, введите ваше ФИО (например, Иванов Иван Иванович):");
                userStates.put(userId, STATE_WAITING_FOR_FIO); // Устанавливаем состояние ожидания ФИО
            } else if (callbackData.equals("privacy_policy")) {
                sendMessage(chatId, "Вы перешли по ссылке на политику конфиденциальности.");
            } else if (callbackData.equals("male") || callbackData.equals("female")) {
                // Обработка выбора пола
                String gender = callbackData.equals("male") ? "Мужской" : "Женский";
                userData.put(userId + "_gender", gender); // Сохраняем пол
                sendMessage(chatId, "Спасибо! Ваш пол: " + gender + ". Теперь отправьте вашу фотографию.");
                userStates.put(userId, STATE_WAITING_FOR_PHOTO); // Переходим к состоянию ожидания фотографии
            }
        }
    }


    @Override
    public String getBotUsername() {
        return System.getenv("TELEGRAM_BOT_NAME");
    }

    @Override
    public String getBotToken() {
        return System.getenv("TELEGRAM_BOT_TOKEN");
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    /**
     * Функция обеспечивает передачу сообщения
     * в чат с Пользователем.
     * @param chatId идентификатор чата или пользователя Telegram
     * @param text текст сообщения
     * @return void
     */
    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    /**
     * Функция обеспечивает приветственное сообщение и предложение
     * о принятии политики ПДн.
     * @param chatId идентификатор чата или пользователя Telegram
     * @return void
     */
    private void sendWelcomeMessage(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Добро пожаловать! Пожалуйста, ознакомьтесь с политикой конфиденциальности и согласитесь на обработку персональных данных. Бот не сохраняет ваши персональные данные!");

        // Создаем inline-кнопки
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Политика конфиденциальности"
        InlineKeyboardButton privacyButton = new InlineKeyboardButton();
        privacyButton.setText("Политика конфиденциальности");
        privacyButton.setUrl("https://example.com/privacy-policy"); // Замените на вашу ссылку

        // Кнопка "Согласиться"
        InlineKeyboardButton agreeButton = new InlineKeyboardButton();
        agreeButton.setText("Согласиться");
        agreeButton.setCallbackData("agree");

        // Добавляем кнопки в строки
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(privacyButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(agreeButton);
        rows.add(row2);

        // Устанавливаем клавиатуру
        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Функция обеспечивает пола у Пользователя
     * @param chatId идентификатор чата или пользователя Telegram.
     * @return void
     */
    private void sendGenderSelection(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите ваш пол:");

        // Создаем inline-кнопки
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка "Мужской"
        InlineKeyboardButton maleButton = new InlineKeyboardButton();
        maleButton.setText("Мужской");
        maleButton.setCallbackData("male");

        // Кнопка "Женский"
        InlineKeyboardButton femaleButton = new InlineKeyboardButton();
        femaleButton.setText("Женский");
        femaleButton.setCallbackData("female");

        // Добавляем кнопки в строку
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(maleButton);
        row.add(femaleButton);
        rows.add(row);

        // Устанавливаем клавиатуру
        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Функция обеспечивает валидацию ФИО у пользователя.
     * @param fio введенные пользователем ФИО в строку
     * @return true/false - признак валидации
     */
    private boolean validateFIO(String fio) {
        // Регулярное выражение для проверки ФИО с учетом символа "-"
        String regex = "^[А-ЯЁ][а-яё]+(-[А-ЯЁ][а-яё]+)?\\s[А-ЯЁ][а-яё]+(-[А-ЯЁ][а-яё]+)?\\s[А-ЯЁ][а-яё]+(-[А-ЯЁ][а-яё]+)?$";
        return Pattern.matches(regex, fio);
    }

    /**
     * Функция обеспечивает валидацию даты рождения по
     * маске dd.MM.yyyy
     * @param birthdate введенные пользователем ФИО в строку
     * @return true/false - признак валидации
     */
    private boolean validateBirthdate(String birthdate) {
        // Регулярное выражение для проверки формата dd.MM.yyyy
        String regex = "^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[012])\\.(19|20)\\d\\d$";

        // Проверяем формат с помощью регулярного выражения
        if (!Pattern.matches(regex, birthdate)) {
            return false; // Формат неверный
        }

        try {
            // Парсим введенную дату
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate inputDate = LocalDate.parse(birthdate, formatter);

            // Получаем текущую дату
            LocalDate currentDate = LocalDate.now();

            // Проверяем, что введенная дата не позже текущей
            return !inputDate.isAfter(currentDate);
        } catch (DateTimeParseException e) {
            return false; // Если парсинг не удался (например, некорректная дата)
        }
    }
    /**
     * Функция обеспечивает формирование документа в
     * формате .docx.
     * @param userId идентификатор  Пользователя Telegram
     * @param birthdate дата рождения Пользователя
     * @param gender пол Пользователя
     * @param fio ФИО Пользователя
     * @param photoFileId ссылка на фотографию в Telegram
     * @return void
     */
    public void createUserDocument(long userId, String fio, String birthdate, String gender, String photoFileId) throws IOException, InvalidFormatException {
        // Создаем новый документ Word
        XWPFDocument document = new XWPFDocument();

        // Добавляем заголовок
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("Данные пользователя");
        titleRun.setBold(true);
        titleRun.setFontSize(16);

        // Добавляем ФИО
        XWPFParagraph fioParagraph = document.createParagraph();
        XWPFRun fioRun = fioParagraph.createRun();
        fioRun.setText("ФИО: " + fio);
        fioRun.setFontSize(14);

        // Добавляем дату рождения
        XWPFParagraph birthdateParagraph = document.createParagraph();
        XWPFRun birthdateRun = birthdateParagraph.createRun();
        birthdateRun.setText("Дата рождения: " + birthdate);
        birthdateRun.setFontSize(14);

        // Добавляем пол
        XWPFParagraph genderParagraph = document.createParagraph();
        XWPFRun genderRun = genderParagraph.createRun();
        genderRun.setText("Пол: " + gender);
        genderRun.setFontSize(14);

        // Добавляем фотографию
        if (photoFileId != null) {
            // Загружаем фотографию по fileId
            byte[] photoBytes = downloadPhoto(photoFileId);
            if (photoBytes != null) {
                XWPFParagraph photoParagraph = document.createParagraph();
                XWPFRun photoRun = photoParagraph.createRun();
                photoRun.addPicture(new ByteArrayInputStream(photoBytes), XWPFDocument.PICTURE_TYPE_JPEG, "photo.jpg", Units.toEMU(200), Units.toEMU(200)); // Размер фото: 200x200
            }
        }

        // Сохраняем документ
        String fileName = "user_" + userId + ".docx";
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            document.write(out);
        }

        document.close();
    }
    /**
     * Функция обеспечивает выгрузку фотографии
     * из Telegram.
     * @param fileId ссылка на фотографию в Telegram
     * @return файл фотографии
     */
    public byte[] downloadPhoto(String fileId) {
        try {
            // Получаем информацию о файле
            File file = execute(new GetFile(fileId));
            // Загружаем файл
            java.io.File downloadedFile = downloadFile(file);
            return Files.readAllBytes(downloadedFile.toPath());
        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Функция очищает временные данные о Пользователе
     * @param userId идентификатор  Пользователя Telegram
     * @return void
     */
    public void deleteUserData(Long userId){
        // Удаляем все данные пользователя
        userData.remove(userId + "_fio");
        userData.remove(userId + "_birthdate");
        userData.remove(userId + "_gender");
        userData.remove(userId + "_photo");

        //Очищаем состояния пользователя
        userStates.remove(userId);

    }

    /**
     * Функция обеспечивает проверку вновь обратившегося Пользователя
     * в бот.
     * @param userId идентификатор Пользователя Telegram
     * @param chatId идентификатор Пользователя Telegram
     * @param update событие
     * @return void
     */
    private void checkUser(long userId, String chatId, Update update) {
        // Получаем UTM-ссылку (если есть)
        String utmLink = extractUtmLink(update); // извлечение UTM-ссылки

        // Проверяем, существует ли пользователь в базе данных
        if (!UserRepository.userExists(userId)) {
            // Если пользователь новый, сохраняем его в базу данных
            UserRepository.insertUser(userId, false, utmLink); // isPolicyAccepted = false по умолчанию
            sendMessage(chatId, "Добро пожаловать! Вы новый пользователь.");
        } else {
            // Если пользователь уже существует, обновляем UTM-ссылку
            UserRepository.updateUserLink(userId, utmLink);
            sendMessage(chatId, "С возвращением! Ваша UTM-ссылка обновлена.");
        }
    }
    /**
     * Функция обеспечивает парсинг UTM-ссылки
     * @param update событие
     * @return ссылка UTM
     */
    private String extractUtmLink(Update update) {
        // Пример извлечения UTM-ссылки из сообщения
        // Например, если пользователь перешел по ссылке https://t.me/your_bot?utm_source=telegram
        String text = update.getMessage().getText();
        if (text.contains("utm_source=")) {
            return text; // Возвращаем всю ссылку или только UTM-часть
        }
        return null; // Если UTM-ссылка отсутствует
    }

}
