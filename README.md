# Freezing Teams

Assign freezing teams based on individual performance at the start of the competition.

## Install scala build tool

[Install sbt](https://www.scala-sbt.org/1.x/docs/Setup.html).

## Run it

Yes, the quotes are sadly required.

```
sbt "run data/captains-2021.csv data/points-2021-01-07.csv assignments.csv"
```

This will write out `assignments.csv` with the team assignments.

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

The athletes CSV file should contain the athlete ids and their points as of some date.

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

Remember to filter out the non-competitors and add zero-point rows for any athletes who (:shame:) failed to ride.

### Assignments

The assignments file will contain team id and athlete id.

```
Team,Athlete
1,101
1,109
```

Team ids are somewhat arbitrarily assigned.
