# F2 - Firsts steps

## Streams Introduction

 Modern applications are often built on top of streaming data: 
 Reading from a huge file and processing information or handling continuous data from the network as WebSockets or from a message broker.

*Streams are the way to go in such situations, and Scala provides its own streams implementation.*

#
**Streams are abstraction to manage an unbounded amount of data**
#

However, streams in the standard library are not as powerful as they could be and don’t offer concurrency, throttling, or backpressure features.

***Fortunately, some libraries offer more robust implementation of streams. One of these is the fs2 library, built on top of the Cats and Cats Effect libraries. Moreover, fs2 provides an entirely functional approach to stream processing.***

[FS2 Tutorial: More than Functional Streaming in Scala](https://blog.rockthejvm.com/fs2/)

## F2 main features

    - Functional
    - Effectful
    - Concurrent
    - I/O (networking, files) computations in constant memory
    - Stateful transformations
    - Resource safety and effect evaluation

## Streams

   * **Pure Streams**: ***Stream[Pure, A]***
     * Store actual data
     * Don't do anything
     * Using the Pure effect means that pulling the elements from the stream cannot fail.

As we will see in a moment, all the streams’ definitions are referentially transparent and remain pure since no side effects are performed.

   * **Effectful Streams**: ***Stream[IO, A]***

## Chunks

Inside, every stream is made of chunks. A Chunk[O] is a finite sequence of stream elements of type O stored inside a structure optimized for indexed based lookup of elements.

## Pipe
Pipe is a type alias for the function Stream[F, I] => Stream[F, O]

**Represents nothing more than a function between two streams**

## Resource Managemen

The fs2 library implements the bracket pattern to manage resources.