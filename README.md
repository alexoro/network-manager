## Network Manager
Android library that allows observe connection state and execute tasks, when network conditions will be acceptable for task (i.e. await while connection will be established).


## How to include in project
Library is distributed via jitpack.io

```gradle
// Add this lines into your roou build.gradle
allprojects {
    repositories {
        jcenter()
        maven { url "https://jitpack.io" }
    }
}
```

```gradle
// Add dependency to library in any target project module
dependencies {
    compile 'com.github.alexoro:network-manager:VERSION'
}
```


## Usage (Google based example)
NetworkManager can be accessible via new instance or via predefined singleton.
```java
NetworkManagerSingleton.init(appContent);

NetworkManager networkManager = NetworkManagerSingleton.get();
// or
// NetworkManager networkManager = new NetworkManager(appContext);

// You can subscribe to network state change.
// Do not forget to unsubscribe, when you are done.
// Not doing this will force memory leak.
networkManager.registerStateListener(...);
networkManager.unregisterStateListener(...);

// Special conditions, when task will be executed
NetworkCondition condition = new NetworkCondition();
condition.isAwaitConnection = true;
condition.isAllowInRoaming = true;

// Execute the task.
// If isAwaitConnection is set to true, then execute will hang,
// until the connection will be established.
networkManager.execute(<callable>, condition);
```
