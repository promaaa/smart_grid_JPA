This documents lists and gives examples for each route the backend should implement.

## From sensor to backend

### HTTP POST `/ingress/windturbine`

Complexity: medium

Used by wind turbine sensors to push data to the server.
The request payload is a JSON string with the following structure:

```json
{
    "windturbine": 1, // turbine ID in database
    "timestamp": 1741334062, // date of the measure
    "data": {
        "speed": 123.4, // speed of the turbine blade in rpm
        "power": 123.4  // generated power by the turbine in W
    }
}
```

Route should process the json, save its data.
Note that you also should compute the new `total_energy_produced` value based on the power reported (you can assume the interval between two datapoints to always be 60 seconds).

If there is no wind turbine with the id provided route must respond with a 404 error.
If there is an error in the json payload route must respond with a 500 error.

If everything is correct, route must respond with the following JSON:

```json
{
    "status": "success"
}
```


### UDP 12345 entrypoint 

Complexity: medium

Used by solar panel sensors to push data to the server.

The payload is a `:` separated string with the following structure: `id:temperature:power:timestamp`.

For instance, `2:25.4:523.6:1741334062` is parsed in:
- solar panel ID: 2
- temperature: 25.4°C
- power: 523.6W
- timestamp: 1741334062

Similarly to the wind turbine, you should also create a new datapoint for `total_energy_produced`.

Sensors do not expect any response.

## From backend to the frontend

General comment, when returning JSON description of entities. If no other format is specified you must convert the list of object to a list of their IDs.

For instance, in the entity `Person`, the relation `Person::grid` is represented as a simple integer and `Person::sensors` is represented by an array of integers.


### GET `/persons`

Complexity: easy

Return a JSON array containing all person IDs in database.

Example response [for this request](http://localhost:8080/persons):
```json
[1, 2]
```

### GET `/person/:id`

Complexity: easy

Return a JSON object that describes the person.

Example response [for this request](http://localhost:8080/persons/1)
```json
{
  "id": 1,
  "first_name": "George",
  "last_name": "Abitbol",
  "grid": 1,
  "owned_sensors": [
    1,
    2
  ]
}
```

Route should return 404 if no person with the provided ID exist.

### POST `/person/:id`

Complexity: hard

Updates the person with the corresponding id in database.
Expect a JSON payload containing the new values. If an attribute is present in the JSON its value overrides the old one.
If an attribute is missing in the JSON the old value is kept unchanged.


Example of JSON payload:
```json
{
  "first_name": "Émile",
  "last_name": "Zola",
  "grid": 1,
  "owned_sensors": [1, 2]
}
```


Returns 404 if no person corresponds to the id.
Returns 200 if the update is successful.
Returns 500 if an error occurred during update.

### DELETE `/person/:id`

Complexity: medium

Deletes the person with the corresponding id.

Returns 200 if the person has been suppressed without error.
Returns 404 error if no person with this id exists.
Returns 500 if an error occurred during suppression.


### PUT `/person`

Complexity: hard

Add a new person to the database.

This route expects a JSON formatted payload that contains the data of the person to create.
Attributes `grid`, `first_name` and `last_names` are mandatory, `owned_sensors` is optional.


```json
{
  "first_name": "Émile",
  "last_name": "Zola",
  "grid": 1,
  "owned_sensors": [1, 2]
}
```


Returns a 500 error if no JSON is provided or if first name, last name or grid are missing.
Returns a JSON object with the id of the created person.

Exemple response:
```json
{
  "id": 42
}
```

### GET `/grids`

Complexity: easy

Returns a list of al grids ID.

Example result of `/grids`:
```json
[1]
```

### GET `/grid/:id`

Complexity: easy

Returns a JSON description of the grid with the specified ID.

Example of `/grid/:id`:
```json
{
  "id": 1,
  "name": "La Chantrerie",
  "description": "smart grid du quartier de la Chantrerie",
  "users": [
    1,
    2
  ],
  "sensors": [
    3,
    4,
    1,
    5,
    2,
    6
  ]
}
```

Returns a 404 error if id no grid with corresponding id is found
Return a JSON object otherwise


### GET `/grid/:id/production` and `/grid/:id/consumption`

Complexity: hard

Returns the total production / consumption of the grid.

Exemple of `/grid/1/production`:
```json
4439476.1618367
```

Example of `/grid/1/consumption`:
```json
1605381.1528419708
```

Returns 404 if no grid with the id is found,
Returns the value otherwise.

### GET `/sensor/:id`

Complexity: easy / hard -- TODO


Return a JSON object containing all details of sensor with the given `id`.
Response MUST contain all specific informations, e.g. `blade_length` for a wind turbine or `efficiency` for a solar panel.

Example `/sensor/2` response:
```json
{
  "id": 2,
  "name": "Éolienne 1",
  "description": "l'éolienne à côté de Polytech",
  "kind": "WindTurbine",
  "grid": 1,
  "available_measurements": [
    3,
    4
  ],
  "owners": [
    1,
    2
  ],
  "power_source": "wind",
  "height": 30.0,
  "blade_length": 10.0
}
```

Example `/sensor/4` response:
```json
{
  "id": 4,
  "name": "Chargeur 2",
  "description": "un autre chargeur de voitures",
  "kind": "EVCharger",
  "grid": 1,
  "available_measurements": [],
  "owners": [],
  "max_power": 600.0,
  "type": "type 2",
  "maxAmp": 20,
  "voltage": 380
}
```

Returns 404 if the id does not correspond to a sensor.

### GET `/sensors/:kind`

Complexity: medium

Return a JSON array of all IDs of sensors with the given kind.
Valid kinds are:
- SolarPanel
- WindTurbine
- EVCharger

Example `/sensors/EVCharger`
```json
[ 3, 4 ]
```

It returns an empty list if kind is not correct or if no sensors of this is found.


### GET `/consumers`, GET `/producers`

Complexity: easy / hard

Return a JSON object containing a list of all consumers (resp. producers).

Example `/producers` response:
```json
[
  {
    "id": 1,
    "name": "panneau solaire 1",
    "description": "une description",
    "kind": "SolarPanel",
    "grid": 1,
    "available_measurements": [
      1,
      2
    ],
    "owners": [
      1
    ],
    "power_source": "solar",
    "efficiency": 0.0
  },
  {
    "id": 2,
    "name": "Éolienne 1",
    "description": "l'éolienne à côté de Polytech",
    "kind": "WindTurbine",
    "grid": 1,
    "available_measurements": [
      3,
      4
    ],
    "owners": [
      1,
      2
    ],
    "power_source": "wind",
    "height": null,
    "blade_length": null
  },
  ...
]
```

FIXME: specs & frontend expect to have all specific info of sensors daughters classes, but backend only send sensor+{consumer,producer} info, leaving specific info with default value (see SQL native queries)



### POST `/sensor/:id`

Complexity: hard

Updates a sensor with the given id.
Expects a JSON payload with the new informations.
Any field of the sensor can be updated. Fields present in the JSON that do not exist in the sensor are ignored (e.g. an `height` field for and EV Charger).

Exemple of JSON payload:
```json
{
  // for all sensors
  "name": "new name of the sensor",
  "description": "new description",
  "owners": [1, 2],

  // if sensor is a producer
  "power_source": "a string",

  // if sensor is a consumer
  "max_power": 123.456,

  // if sensor is a solar panel
  "efficiency": 123.456,

  // if sensor is a wind turbine
  "height": 12.2,
  "blade_length": 123.2,

  // if sensor is an EV charger
  "type": "AC",
  "voltage": 230,
  "maxAmp": 200
}
```



Returns 404 if not sensor with id exists
Returns 200 if update is successful


### GET `/measurement/:id`

Complexity: easy

Returns a JSON object representing a measurement with the given id.

Example of `/measurement/1`: 
```json
{
  "id": 1,
  "sensor": 1,
  "name": "temperature",
  "unit": "C°"
}
```

Returns a 404 error if no measurement with given id is found.

### GET `/measurement/:id/values`

Complexity: medium


Returns a JSON object representing a measurement and its values.
`from` and `to` query parameters can be used to limit values returns.
If `from` is not specified, default to 0.
If `to` is not specified, default to 2147483646.

Exemple `/measurement/1/values`:
```json
{
  "sensor_id": 1,
  "measurement_id": 1,
  "values": [
    {
      "timestamp": 1743509245,
      "value": 20.2841017662049
    },
    {
      "timestamp": 1743509305,
      "value": 19.3168922700183
    },
...
    {
      "timestamp": 1743510447,
      "value": 56.09000015258789
    },
    {
      "timestamp": 1743510507,
      "value": 15.859999656677246
    }
  ]
}
```

Example of `/measurement/1/values?from=1743510147&to=1743510207`
```json
{
  "sensor_id": 1,
  "measurement_id": 1,
  "values": [
    {
      "timestamp": 1743510205,
      "value": 17.0622955246169
    },
    {
      "timestamp": 1743510147,
      "value": 63.79999923706055
    },
    {
      "timestamp": 1743510207,
      "value": 33.29999923706055
    }
  ]
}
```

