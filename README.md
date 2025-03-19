# Freezing Teams

Assign freezing teams based on individual performance at the start of the competition and optionally
performance at the end of the last competition and optionally spatial locality of the team.

## Install scala build tool

[Install sbt](https://www.scala-sbt.org/1.x/docs/Setup.html).

## Run it

Yes, the quotes are sadly required.

```
sbt "run --captains data/captains-2021.csv --athletes data/athletes-2021.csv --points data/points-2021-01-07.csv --out assignments.csv"
```

This will write out `assignments.csv` with the team assignments based just on year-to-date performance.

### Prior year weighting

To factor in prior-year points, provide a CSV with the prior points and choose a weighting for the
prior year (0.33 to weight prior year 33%, current year 67%). This calculation needs to know how
many days of competition the current and prior CSVs represent; by default it assumes 7 days for
the current year and uses astrological divination for the prior year. If prior data does not
exist for an athlete, just their current year performance is considered.


```
sbt "run --captains data/captains-2021.csv --points data/points-2021-01-07.csv --prior data/points-2020-03-19.csv --out assignments.csv"
```

Or, to be pedantic:

```
sbt "run --captains data/captains-2021.csv --points data/points-2021-01-07.csv --prior data/points-2020-03-19.csv --pointsDays 7 --priorDays 58 --priorWeight 0.5 --out assignments.csv"
```

### Zip code weighting

To factor in spatial locality of the team, specify a zip codes file with zip code latitudes and longitutes.
This will take more time to process.

```
sbt "run --captains data/captains-2021.csv --athletes data/athletes-2021.csv --points data/points-2021-01-07.csv --zipCodes US.csv --out assignments.csv --map map.csv"
```

The locality is weighted by the scaled distance; by default 1 mile is treated like 1 point. A map file is
output, suitable for upload to Google My Maps.

## CSV format

The CSVs need headers.

### Captains

The captains CSV file should just contain the captain ids.

```
Captain
101
202
303
404
```

### Athletes

The athletes CSV file should contain the athlete ids and names (including captains) and their zip codes. Assumes
column titles "Strava user ID", "Name", "Email", "Zip Code".

```
Athlete,Name,Email,,,,,,,,,,,,,,,Zip
101,Chris Christofferson,chris@example.org,,,,,,,,,,,,,,,12345
202,Kris Kristey,kris@example.org,,,,,,,,,,,,,,,54321
303,Crystal Maze,crys@example.org,,,,,,,,,,,,,,,31415
404,Krys Kringle,Krys@example.org,,,,,,,,,,,,,,,98765
```

### Points

The points CSV file should contain the athlete ids and their points as of some date.

```
Athlete,Points
101,12.34
109,23.45
181,34.56
829,45.67
```

Pull this from the database as, for example:

```mysql
SELECT   athlete_id, SUM(points)
FROM     daily_scores
WHERE    ride_date <= '2021-01-07'
GROUP BY athlete_id
```

### Prior points

The prior points CSV should look just like the athletes CSV. Don't fill in with zeroes; if an athlete is missing
then only their current performance is considered.

### Zip codes

This should be a CSV with the zip code, latitude and longitude. Take the US TSV from http://download.geonames.org/export/zip/
and make a CSV of it with a header row.

### Assignments

The assignments file will contain team id and athlete id.

```
Team,Strava ID,Name,Email,Captain
1,101,Chris,chris@example.org,Yes
1,109,Kris,kris@example.org,
```

Team ids are somewhat arbitrarily assigned.
