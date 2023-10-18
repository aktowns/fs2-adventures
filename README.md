# fs2-adventures
In general you should complete the exercises in the master branch.
There is a solution branch with  ... guess what...

The exercises below are designed to be followed in order.

Please note these exercises are using cats effect (IO) and FS2 3.9.2.  Please refer to the docs at
https://fs2.io/#/guide

## Adventure 1: Asynchrony with IO
The aim is:

1. Gain practice/confidence in working with IO.

Complete the exercises in: adventures.io.IOAdventures

There are a series of tests that you need to make pass in IOAdventuresSpec.
Run them with `sbt "testOnly *.IOAdventuresSpec"`.  Tests can also be run in the IDE.


## Adventure 2: Asynchrony with FS2 Stream
The aim is:

1. Gain practice/confidence in working with FS2 Stream.

Where IO is for a single asynchronous action, a `Stream` represents an stream of
asynchronous actions.

Complete the exercises in: adventures.stream.StreamAdventures

The docs for `Stream` are online https://fs2.io/#/guide?id=building-streams.  

There are a series of tests that you need to make pass in StreamAdventuresSpec.
Run them with `sbt "testOnly *.StreamAdventuresSpec"`.  Tests can also be run in the IDE.
