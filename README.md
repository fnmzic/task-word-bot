# Инструкция по сборке и развертыванию Telegram-бота

Этот проект представляет собой Telegram-бота, который использует базу данных PostgreSQL для хранения данных. Проект развертывается с помощью Docker и Docker Compose.


# Предварительные требования

1.  Установите  [Docker](https://docs.docker.com/get-docker/).
    
2.  Установите  [Docker Compose](https://docs.docker.com/compose/install/).

## Быстрый старт

1.  Склонируйте репозиторий:
    - git clone https://github.com/fnmzic/task-word-bot.git
    
    - cd task-word-bot
    
3.  Запустите проект:
    docker-compose up --build
    
    Эта команда:
    -   Соберет образ бота из  `Dockerfile`.
    -   Запустит контейнеры для бота и базы данных PostgreSQL.
    -   Подключит бота к базе данных.
        
4.  Проверьте логи бота, чтобы убедиться, что он успешно запустился:  
    docker-compose logs -f bot

##  Переменные окружения

Все необходимые переменные окружения уже указаны в `docker-compose.yml`.  Вам нужно изменить их значения для своего бота и базы данных, отредактируйте файл `docker-compose.yml` напрямую.
