# Repository concurrency model

- Every repository has a coroutine scope named `coroutineScope` initialized using
  `repositoryCoroutineScope()`
- Repositories have two kinds of public functions: async and suspend.
    - Async functions update some state but don't return a value
    - Suspend functions do some work and then return a value
- All functions are main-safe, meaning that they can be called from the main thread without blocking
  it
    - For async functions this means that they immediately launch a new coroutine into
      `coroutineScope` and then return
    - For suspend functions this means that they immediately switch to `Dispatchers.Default`
- Private functions are assumed to be called from `Dispatchers.Default`, but they switch to
  `Dispatchers.IO` for any blocking IO operations using the `io { }` utility functions
