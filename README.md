## DS2-Project

To run the project move to the root directory of the repository.

The `run.sh` script will handle all the compilation and the "java-related-stuff" by itself.

You just have to run

```bash
./run.sh experiment <Class:[Participant|Precise|Scuttlebutt]>\
	<OrderingMethod:[None|Newest|Oldest|Breadth|Depth]>\
	<NumberOfParticipants:[Int]>\
	<NumberOfKeys:[Int]>\
	<FlowControl:[true|false]>
```

All the details about what these arguments do and what they mean are in the report.

--

The program will output the metrics in `/tmp/AEP/logs/<experiment_name>`. The *experiment_name* will be dynamic based on the experiment run.

In order to plot the metrics there is a handy python script `src/plot/plot.py`. Just set the path to the folder containing the outputs of the experiments and uncomment all the traces you want to appear in the plots.