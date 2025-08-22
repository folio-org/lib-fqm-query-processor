# lib-fqm-query-processor
Copyright (C) 2023-2024 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

lib-fqm-query-processor is a java library for use by the FOLIO Query Machine. The library can be used to query the derived
tables used for FQM.

This library accepts FQL (FOLIO Query Language) queries and executes them against the derived FQM tables to return a set of results.
First, a query in JSON string form is deserialized into FQL. The FQL is then converted to SQL and run against derived tables
to return a result set.

## Compiling
```bash
mvn clean install
```

## FQL Language Syntax
FQL uses a light version of MongoDB query syntax. The supported operators are described below.

### Field names

Field names can represent a column itself (most typical), or a nested JSON object/array.
For example, the following field names are valid:

- `field1`: represents a column named `field1`
- `field2->inner`: represents the property named `field2` nested within a column named `field2`
- `field3[*]->inner`: represents the property named `field2` nested within any item of the column `field3`

For these examples, example contents of each field could be:

- `field1`: `value1`, `value2`, `value3`, `[0, 2, 4, 7]`, `["foo", "bar", "baz"]`
- `field2`: `{"inner": "value1"}`, `{"inner": "value2"}`, `{"inner": "value3"}`
- `field3`: `[{"inner": "value1"}, {"inner": "value2"}, {"inner": "value3"}]`, `[]`

Note that arrays containing primitive types (not objects) should **not** be queried using this syntax; instead, they should
be queried using the `$contains` and `$not_contains` array operators.

A formal definition of the FQL field name is below:

```
field_name  ::= column_name |
                column_name '->' field_name |
                column_name '[*]'* '->' field_name
column_name ::= [a-zA-Z_][a-zA-Z0-9_]*
```

### $eq
Match values that are equal to a specified value. String comparison is done in a case-insensitive manner. Supports
string, number, boolean, date, uuid, array, and jsonbArray types.

Examples:
```json
{"field1": {"$eq": "value1"}}
```
```json
{"field1": {"$eq": 10}}
```
If a date in the "yyyy-mm-dd" format (i.e., without  time) is provided, the query should match all the records with that specific date, regardless of the time. The following date will match all records that match between Jan 10th 12:00 AM (including) and Jan 11th 12:00 AM (excluding)
```json
{"field1": {"$eq": "2023-01-10"}}
```
If a date and time are both provided, the query will match all the records with that specific date and time. The following date will match all records that match exactly at 2023-01-10 16:32:12 GMT
```json
{"field1": {"$eq": "2023-01-10T16:32:12Z"}}
```

### $ne
Match values that are not equal to a specified value. String comparison is done in a case-insensitive manner. Supports
string, number, boolean, date, uuid, array, and jsonbArray types.

Examples:
```json
{"field1": {"$ne": "value1"}}
```
```json
{"field1": {"$ne": 10}}
```
If a date in the "yyyy-mm-dd" format (i.e., without  time) is provided, the query should match all the records with that specific date, regardless of the time. The following date will match all the entries that don't have dates between Jan 10th 12:00 AM (including) and Jan 11th 12:00AM (excluding)
```json
{"field1": {"$ne": "2023-01-10"}}
```

### $gt
Match values that are greater than a specified value. Supports number and date types.

Examples:
```json
{"field1": {"$gt": "2023-01-12"}}
```

```json
{"field1": {"$gt": 10}}
```

### $gte
Match values that are greater than or equal to a specified value. Supports number and date types.

Examples:
```json
{"field1": {"$gte": "2023-01-12"}}
```

```json
{"field1": {"$gte": 10}}
```

### $lt
Match values that are less than a specified value. Supports number and date types.

Examples:
```json
{"field1": {"$lt": "2023-01-12"}}
```

```json
{"field1": {"$lt": 10}}
```

### $lte
Match values that are less than or equal to a specified value. Supports number and date types.

Examples:
```json
{"field1": {"$lte": "2023-01-12"}}
```

```json
{"field1": {"$lte": 10}}
```

### $in
Matches any of the values specified in an array. Supports array of string, date, number, uuid, boolean, array, and jsonbArray types.

Example:
```json
{"field1": {"$in": ["value1", "value2"]}}
```

### $nin
Matches none of the values specified in an array. Supports array of string, date, number, uuid, boolean, array, and jsonbArray types.

Example:
```json
{"field1": {"$nin": ["value1", "value2"]}}
```

### $and
Joins query clauses with a logical AND. Return records that match all the conditions.
Example:
```json
{
    "$and": [
        {"field1": {"$contains": "test"}},
        {"field2": {"$in": ["value1", "value2"]}},
        {"field3": {"$eq": "value3"}}
    ]
}
```

## Supported operators for arrayType and jsonbArrayType

Array and jsonbArray types support the following operators:

### $eq - Match values that are equal to a specified value
```json
{"field1": {"$eq": "value1"}}
```

To match arrays containing ALL specified values, use multiple `$eq`:
```json
{
  "$and": [
    {"field1": {"$eq": "value1"}},
    {"field1": {"$eq": "value2"}}
  ]
}
```

### $ne - Match values that are not equal to a specified value
```json
{"field1": {"$ne": "value1"}}
```

To exclude arrays containing ALL specified values, use multiple `$ne`:
```json
{
  "$and": [
    {"field1": {"$ne": "value1"}},
    {"field1": {"$ne": "value2"}}
  ]
}
```

### $in - Match any of the values specified in an array
```json
{"field1": {"$in": ["value1", "value2", "value3"]}}
```
```json
{"field2": {"$in": [100, 200, 300, 400]}}
```

### $nin - Match none of the values specified in an array
```json
{"field1": {"$nin": ["value1", "value2", "value3"]}}
```
```json
{"field2": {"$nin": [404, 500, 503]}}
```

### Pattern matching with support for contains and starts with
**Contains** - matches arrays where any element contains the specified substring:
```json
{"field1": {"$contains": "value1"}}
```
Example: If field1 contains `["prefix_value1_suffix", "other_value"]`, it will match because "value1" is contained in the first element.

**Starts with** - matches arrays where any element starts with the specified pattern:
```json
{"field1": {"$starts_with": "value1"}}
```
Example: If field1 contains `["value1_something", "another_value"]`, it will match because the first element starts with "value1".

Additional examples:
```json
{"field1": {"$contains": "test"}}
```
```json
{"field1": {"$starts_with": "prefix"}}
```

### $empty - Check if the field is null or empty
```json
{"field1": {"$empty": true}}
```
```json
{"field2": {"$empty": false}}
```

## Using the Library
The library can be used to run a query against a FOLIO tenant's set of derived tables and to retrieve the results of the query.

### Executing a query asynchronously
A query can be executed using the library's `streamIdsInBatch` method. This method will run a query and return the ids of the results of the query in batches. The method accepts the following parameters:
  - FolioQueryWithContext: Object containing query information. Includes the following fields:
    - tenantId (String): ID of the FOLIO tenant
    - entityTypeId (UUID): the UUID of the entity type for the query
    - fqlQuery (String): the FQL query string
    - sortResults (boolean): whether to sort the results of the query
  - batchSize (int): number of records to retrieve per batch
  - idsConsumer (Consumer): callback function to consume the ids retrieved from the query. Accepts a batch of ids as input
  - successHandler (IntConsumer): Callback function to handle query execution success. Accepts the total number of records returned as input
  - errorHandler (Consumer): Callback function to handle query execution failure. Accepts a throwable as input

### Retrieving the results of an asynchronous query
The full results of a query can be retrieved using the library's `getResultSet` method. This method accepts the following parameters:
  - tenantId (String): ID of the FOLIO tenant
  - entityTypeId (UUID): the UUID of the entity type for the query
  - fields (List of Strings): list of fields from the entity type to return in the query results
  - ids (List of UUIDs): list of UUIDS corresponding to the results of the query

### Executing a query synchronously
A query can also be executed synchronously, returning the results directly rather than passing them off to a callback function.
This can be done using the library's `processQuery` method. The method accepts the following parameters:
  - tenantId (String): ID of the FOLIO tenant
  - entityTypeId (UUID): The UUID of the entity type for the query
  - fqlQuery (String): The FQL query string
  - fields (List of Strings): List of fields from the entity type to return in the query results
  - afterId (UUID): ID corresponding to the element after which results should start being retrieved. Functions as an offset (e.g., if the ID of the 100th element is provided, then results will will be retrieved starting from the 101st element). If this parameter is null, it indicates an offset of 0.
  - limit (int): number of results to retrieve

## More information on entity types
The query processor library relies heavily on the concept of entity types. An entity type contains all the information pertaining to a specific entity in a query. This includes all the fields belonging to an entity, and the datatypes of those fields. Examples of entity types include items, loans, and users. An example of the user entity type is provided below.

### User Entity Type
 - id (UUID): UUID of the user
 - username (String): username of the user in FOLIO
 - user_first_name (String): user's first name
 - user_last_name (String): user's last name
 - user_full_name (String): user's full name
 - user_active (boolean): whether a user is active
 - user_barcode (String): Barcode corresponding to the user
 - user_expiration_date (Date): Date on which a user has or will become inactive
 - user_patron_group_id (UUID): ID of the patron group that a user belongs to
 - user_patron_group (String): Name of the patron group that a user belongs to

## Additional information

### Issue tracker

See project [LIBFQMQUER](https://folio-org.atlassian.net/browse/LIBFQMQUER)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Code of Conduct

Refer to the Wiki
[FOLIO Code of Conduct](https://folio-org.atlassian.net/wiki/x/V5B).

### Code analysis

[SonarQube analysis](https://sonarcloud.io/project/overview?id=org.folio%3Alib-fqm-query-processor)

### Download and configuration

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access
