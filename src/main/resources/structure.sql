CREATE TABLE IF NOT EXISTS `zones` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `areaOwner` TEXT NOT NULL,
  `x` INTEGER NOT NULL,
  `z` INTEGER NOT NULL
);