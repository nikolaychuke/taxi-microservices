# taxi-microservices

Удалить БЗ - docker volume rm taxi_postgres_data
Запуск - docker compose up --build
ВЫКЛ - docker compose down
Запуск тестов - mvn test
Удалить target - mvn clean



ТОКЕН
TOKEN=$(curl -s -X POST http://localhost:8081/auth/token \
-H "Content-Type: application/json" \
-d '{"username":"admin","password":"admin"}' | jq -r '.token')

echo $TOKEN

РЕГИСТРАЦИЯ ПАССАЖИРА
curl -X POST http://localhost:8081/passengers \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"name":"passenger1","email":"passenger1@email.com","phone":"+79990000001"}'

РЕГИСТРАЦИЯ ВОДИТЕЛЯ
curl -X POST http://localhost:8081/drivers \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"name":"driver1","email":"driver1@email.com","phone":"+79990000002","licenseNumber":"LIC123456"}'

ПРОВЕРКА ПРОФИЛЯ ПАССАЖИРА
curl -X GET http://localhost:8081/passengers/1 \
-H "Authorization: Bearer $TOKEN"

ПРОВЕРКА ПРОФИЛЯ ВОДИТЕЛЯ
curl -X GET http://localhost:8081/drivers/1 \
-H "Authorization: Bearer $TOKEN"

СОЗДАНИЕ ПОЕЗДКИ
curl -X POST http://localhost:8082/trips \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"passenger_id":1,"origin":"Moscow","destination":"Novosibirsk","distance_km":10}'

ПРОВЕРКА ИНФОРМАЦИИ О ПОЕЗДКЕ
curl -X GET http://localhost:8082/trips/1 \
-H "Authorization: Bearer $TOKEN"

ВОДИТЕЛЬ ПРИНЯЛ ЗАКАЗ
curl -X PATCH http://localhost:8082/trips/1/status \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"status":"ACCEPTED"}'

ПОЕЗДКА
curl -X PATCH http://localhost:8082/trips/1/status \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"status":"STARTED"}'

curl -X PATCH http://localhost:8082/trips/1/status \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"status":"COMPLETED"}'

ОЦЕНКА ПОЕЗДКИ
curl -X PATCH http://localhost:8082/trips/1/rating \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"rating":5}'

ИСТОРИЯ ПОЕЗДОК ПАССАЖИРА
curl -X GET "http://localhost:8082/trips?passenger_id=1" \
-H "Authorization: Bearer $TOKEN"

ПРОВЕРКА УВЕДОМЛЕНИЙ ПО ПОЕЗДКЕ
curl -X GET "http://localhost:8083/notifications?trip_id=1" \
-H "Authorization: Bearer $TOKEN"

СТАТИСТИКА ЗА ДЕНЬ
curl -X GET "http://localhost:8082/trips/stats/daily?date=$(date +%Y-%m-%d)" \
-H "Authorization: Bearer $TOKEN"

ПРОВЕРКА СТАТУСА ВОДИТЕЛЯ
curl -X GET http://localhost:8081/drivers/1 \
-H "Authorization: Bearer $TOKEN"
