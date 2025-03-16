Hello,

get request to retrieve all products
https://bgm2vmcofb.execute-api.eu-central-1.amazonaws.com/prod/products
get request to retrieve product by id 
https://bgm2vmcofb.execute-api.eu-central-1.amazonaws.com/prod/products/123e4567-e89b-12d3-a456-426614174000Adding a New Product via API

To add a new product, send a POST request to the following endpoint:
Редактировать
https://bgm2vmcofb.execute-api.eu-central-1.amazonaws.com/prod/products
Request Body (JSON)
Include the following JSON payload in the request body:
`{
"title": "Sony PlayStation 5",
"description": "Next-gen gaming console",
"price": 499,
"count": 10
}`
