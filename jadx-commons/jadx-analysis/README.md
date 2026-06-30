## jadx analysis

Various utilities for analyze and process code and related information.


### Call graph

Full app code usage/call graph.
Usage:
```java
JadxArgs args = new JadxArgs();
args.addInputFile(new File("input.apk"));
try (JadxDecompiler jadx = new JadxDecompiler(args)) {
  jadx.load();

  ICallGraph callGraph = JadxCallGraph.builder(jadx)
    .includePackages("com.example") // filter nodes by package
    .resolvedOnly(true) // add nodes only from app (exclude framework/lib calls)
    .build();

  for (ICallGraphEdge edge : callGraph.edges()) {
    if (edge.isResolved()) {
      System.out.printf("Edge from '%s' to '%s'%n", edge.from(), edge.to());
    }
  }
  callGraph.writeDot(Path.of("test.dot")); // export to '.dot'
  callGraph.writeJson(Path.of("test.json")); // export to JSON
}
```
