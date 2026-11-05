# Семинар: учёт студентов

Spring Boot 4.0.2, Gradle Wrapper 9.4.0, Java 17+, MySQL, Spring Data JPA, Thymeleaf и Lombok. Исходный код компилируется с `--release 17`. Gradle 9.4 поддерживает установленный JDK 26; Lombok 1.18.48 выбран с учётом этого JDK.

## Запуск с MySQL

Откройте `seminars/03-students` в IntelliJ IDEA как Gradle-проект. Все следующие команды выполняются из этой папки.

Если MySQL уже установлен, создайте базу:

```sql
CREATE DATABASE IF NOT EXISTS test613 CHARACTER SET utf8mb4;
```

По условию `application.properties` содержит локальные значения: URL `jdbc:mysql://localhost:3306/test613`, пользователь `root`, пароль `1111`, драйвер `com.mysql.cj.jdbc.Driver`. Hibernate создаёт таблицу `Student`, колонку `ID` и поля `firstName`, `lastName`; включены `ddl-auto=update`, вывод и форматирование SQL. Настройки подключения можно переопределить через `DB_URL`, `DB_USER`, `DB_PASSWORD`.

```sh
./gradlew bootRun
```

Если MySQL нет, можно использовать отдельный контейнер (нужен работающий Docker):

```sh
docker compose up -d --wait
DB_URL='jdbc:mysql://localhost:3307/test613?useUnicode=true&characterEncoding=UTF-8' ./gradlew bootRun
```

Контейнер использует порт **3307**, чтобы не занимать порт MySQL домашних работ. База сохраняется в Docker-томе. Остановка: `docker compose stop`.

Откройте http://localhost:8085/web/students. Страница `students.html` выводит таблицу ID, FirstName, LastName через `th:each`. Через форму можно создавать студентов, изменять имена и удалять записи. ID - положительное число, вводимое вручную по условию. Начальная база пуста. Сервер принимает Java Long; форма ограничивает ввод безопасными целыми JavaScript.

В IntelliJ можно также запустить `StudentsApplication`. Для остановки приложения в терминале нажмите Ctrl+C. Порт меняется переменной `SERVER_PORT`.

## REST API

| Метод | URL | Результат |
| --- | --- | --- |
| GET | `/db/students` | Список студентов по ID |
| GET | `/db/students/{id}` | Один студент |
| POST | `/db/students` | Создать: `id`, `firstName`, `lastName`; ответ 201 |
| PUT | `/db/students/{id}` | Обновить: `firstName`, `lastName`; ID берётся из URL |
| DELETE | `/db/students/{id}` | Удалить; ответ 204 |
| GET | `/web/students` | HTML-таблица Thymeleaf |

```sh
curl -i http://localhost:8085/db/students -H 'Content-Type: application/json' -d '{"id":1,"firstName":"Иван","lastName":"Иванов"}'
curl http://localhost:8085/db/students
curl http://localhost:8085/db/students/1
curl -X PUT http://localhost:8085/db/students/1 -H 'Content-Type: application/json' -d '{"firstName":"Пётр","lastName":"Петров"}'
curl -X DELETE http://localhost:8085/db/students/1
```

Неизвестный ID возвращает 404, повторный ID при создании - 409, пустые имена или неверный ID - 400. `StudentService.findById` пишет INFO при успешном поиске и WARN, если студента нет. Вызовите GET для существующего и отсутствующего ID, чтобы увидеть оба сообщения в консоли.

## Проверки

```sh
./gradlew clean build
```

Интеграционные HTTP/JPA-тесты запускаются на изолированной H2 в режиме MySQL: CRUD, рендеринг Thymeleaf, пустой список, экранирование HTML, валидация, дубли ID, 404 и логирование. H2 доступна только тестам. Работу с реальной MySQL проверяйте отдельно запуском основного профиля.

Самостоятельная Gradle-сборка семинара не входит в корневую Maven-сборку домашних работ.
