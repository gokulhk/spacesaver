package io.github.gokulhk.spacesaver.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/** A fresh in-memory database; queries run on Room's executors like in production. */
internal fun inMemoryDatabase(): SpaceSaverDatabase =
    Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SpaceSaverDatabase::class.java)
        .allowMainThreadQueries()
        .build()
