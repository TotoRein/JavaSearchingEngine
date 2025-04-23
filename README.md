# SearchEngine
SearchEngine - итоговый проект курса "Java-разработчик". Приложение предствляет собой локальный поисковый движок по сайту, позволяющий индексировать страницы и осуществлять по ним поиск. Движок разрабатывается на фреймворке Spring с использованием многопоточности.

# Технологии
* [Spring](https://spring.io/)
* [MySQL](https://www.mysql.com/)
* [Liquibase](https://www.liquibase.com/)
* [Maven](https://maven.apache.org/)
* [Hibernate](https://hibernate.org/)

# Использование
1. Клонируйте репозиторий
2. Настройте доступ к базе данных в файле application.yaml
3. Обновите Maven-зависимости
4. Создайте базу данных, выбрав метод сравнения (collation) utf8mb4_general_ci для корректной работы с кирилическими символами
5. Укажите список сайтов для индексации в файте application.yaml в формате:
        dir indexing-settings:
        dir   sites:
        dir     - url: http://www.somesite.com/
        dir       name: Sitename or description
7. При запуске проекта таблицы будут созданы liquidbase автоматически в соответствии со схемой, определённой в файле db.changelog-master.xml
8. Web-интерфейс приложения доступен при обращении к порту 8080
