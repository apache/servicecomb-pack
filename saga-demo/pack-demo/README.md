# Pack Transaction Demo
This demo simulates a booking application including three services:
* pack-booking
* pack-car
* pack-hotel

## Running Demo
1. run the following command to create docker images in saga project root folder.
```
mvn package -DskipTests -Pdocker -Pdemo
```

2. start application up in saga/saga-demo/pack-demo with the following command
```
docker-compose up
```

## User Requests
1. Booking 2 rooms and 2 cars, this booking will be OK.
```
curl -X POST http://{docker.host.ip}:8083/booking/test/2/2
```
Check the hotel booking status with
```
curl http://{docker.host.ip}:8081/bookings
```
Check the car booking status with
```
curl http://{docker.host.ip}:8082/bookings

```

2. Booking 3 rooms and 2 cars, this booking will case the hotel order failed and trigger the compansate operation with car booking.
```
curl -X POST http://{docker.host.ip}:8083/booking/test/3/2
```
Check the hotel booking status with
```
curl http://{docker.host.ip}:8081/bookings
```
Check the car booking status with
```
curl http://{docker.host.ip}:8082/bookings
```
The second car booking will be marked with **cancel:true**


**Note** transactions and compensations implemented by services must be idempotent.
