# API по загрузке сертификатов в БД

## Требования к ПО
### ОС: WindowsNT и выше
### Server: Tomcat9

## Структура БД
### Таблица Certificates
#### Столбцы: 
	id [int] IDENTITY(1,1) NOT NULL
	bsf [nvarchar](max) NULL
	ownerORVi [nvarchar](255) NULL
	dtFrom [datetime] NULL
	dtTo [datetime] NULL
	serial [nvarchar](255) NULL
	thumbPrint [nvarchar](255) NULL
	company [nvarchar](255) NULL
	activeFlag [bit] NULL
	changeDate [datetime] NULL getdate()
	inn [nvarchar](50) NULL
	kpp [nvarchar](50) NULL
	ogrn [nvarchar](50) NULL

## Структура данных запроса
### Файлы формата pfx в бинарном виде посредством WebForm

## Структура данных строки ответа
### json: [{"Status":"<данные>","<Description/SerialNumber>":"<данные/серийный номер сертификата>"}]

## Схема работы

### Клиент
#### Формирование запроса -> Отправка на сервер -> Получение ответа сервера

### Сервер
#### Получение запроса -> Обработка запроса -> Отправка клиенту json

## Структура запроса
### Тип: POST
### Заголовки
#### Content-Type: multipart/form-data

## Структура ответа
### Заголовки
#### Content-Type: application/json;charset=windows-1251