# The application database isn't working

You have installed Metabase, but:

- it logs a `liquibase` error message when you try to run it,
- it logs another error message that mentions `H2` or `h2` while it is running, or
- you are on Windows 10 and get a warning about file permissions.

## Are you using H2 as your application database?

**Root cause:** Metabase stores information about users, questions, and so on in a database of its own called the "application database", or "app database" for short. By default Metabase uses [H2][what-is-h2] for the app database, but we don't recommended it for production---because it's an on-disk database, it's sensitive to filesystem errors, such as a drive being corrupted or a file not being flushed properly.

**Steps to take:**

1. FIXME how to tell if you're using H2
2. See [Migrating from H2][migrating] for instructions on how to migrate to a more robust app database.

## Is the app database locked?

**Root cause:** Sometimes Metabase fails to start up because an app database lock did not clear properly during a previous run. The error message looks something like:

```
liquibase.exception.DatabaseException: liquibase.exception.LockException: Could not acquire change log lock.
```

**Steps to take:**

1.  Open a shell on the server where Metabase is installed and manually clear the locks by running:

    ```
    java -jar metabase.jar migrate release-locks
    ```

2.  Once this command completes, restart your Metabase instance normally (*without* the `release-locks` flag).

## Is the app database corrupted?

**Root cause:** H2 is less reliable than production-quality database management systems, and sometimes the database itself becomes corrupted. This can result in loss of data in the app database, but can *not* damage data in the databases that Metabase is connected.

**Steps to take:** Error messages can vary depending on how the app database was corrupted, but in most cases the log message will mention `h2`. A typical command and message are:

```
myUser@myIp:~$ java -cp metabase.jar org.h2.tools.RunScript -script whatever.sql -url jdbc:h2:~/metabase.db
Exception in thread "main" org.h2.jdbc.JdbcSQLException: Row not found when trying to delete from index """"".I37: ( /* key:7864 */ X'5256470012572027c82fc5d2bfb855264ab45f8fec4cf48b0620ccad281d2fe4', 165)" [90112-194]
    at org.h2.message.DbException.getJdbcSQLException(DbException.java:345)
    [etc]
```

**How to fix this:** not all H2 errors are recoverable (which is why if you're using H2, _please_ have a backup strategy for the application database file).

1.  Determine whether you are running a recent version of Metabase or an older version:
2.  If you are running a recent version and using H2, the app database is stored in `metabase.db.mv.db`.
    -   Open a shell on the server where the Metabase instance is running and attempt to recover the corrupted H2 file by running:
        ```
        $ java -cp metabase.jar org.h2.tools.Recover
        $ mv metabase.db.mv.db metabase.old.db
        $ touch metabase.db.mv.db
        $ java -cp target/uberjar/metabase.jar org.h2.tools.RunScript -script metabase.db.h2.sql -url jdbc:h2:`pwd`/metabase.db
        ```
3.  If you are running an older version, the app database is stored in `metabase.db.h2.db`.
    -   Open a shell on the server where the Metabase instance is running and attempt to recover the corrupted H2 file by running:
        ```
        $ java -cp metabase.jar org.h2.tools.Recover
        $ mv metabase.db.h2.db metabase.old.db
        $ touch metabase.db.h2.db
        $ java -cp target/uberjar/metabase.jar org.h2.tools.RunScript -script metabase.db.h2.sql -url jdbc:h2:`pwd`/metabase.db;MV_STORE=FALSE
        ```

## Are you running Metabase with H2 on Windows 10?

**Root cause:** In some situations on Windows 10, the Metabase JAR needs to have permissions to create local files for the application database. In this case, you will see an error message like this when running the JAR:

```
Exception in thread "main" java.lang.AssertionError: Assert failed: Unable to connect to Metabase DB.
```

**Steps to take:**

1.  Right-click on the Metabase JAR file (*not* the app database file).
2.  Select "Properties".
3.  Select "Unblock."

[what-is-h2]: ../faq/setup/what-is-h2.html
[migrating]: ../operations-guide/migrating-from-h2.html
