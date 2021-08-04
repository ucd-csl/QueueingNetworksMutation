# Mutation-based Analysis of Performance Models

## Description
Understanding the performance characteristics of complex software systems is a challenging problem, due to many factors that may contribute to fluctuations. Queuing Network models have been used in the past to model and understand the performance characteristics of software systems subject to variations. However, the identification of performance criticalities is still an open challenge, since there might be several system components contributing to the overall performance of the system. This work combines two different areas of research to improve the process of interpreting model-based performance analysis results. On one hand, software performance engineering techniques provide the ground for the evaluation of the system's performance through well-assessed performance models. On the other hand, mutation-based techniques nicely support the experimentation of changes in performance models and contribute a more systematic assessment of performance indices, such as system response time and utilisation of resources. To this end, we propose mutation operators for queuing networks that resemble changes commonly made by designers when exploring the properties of a system's performance.. Our approach consists in introducing a mutation-based approach that generates a set of mutated queuing network models. The performance of these mutated networks is compared to that of the original network under a given set of workloads to better understand the effect of variations in the different components of the system. A set of benchmarks is adopted to show how the technique can be used to get a deeper understanding of the performance characteristics of software systems.

## Structure of the repository
* Folder *code* contains the code implementing the approach: the mutation operators, the interface with the [JMT](http://jmt.sourceforge.net/) simulator, and the component to save the experimental results
* Folder *models* contains the queueing networks used in the experiments
* Folder *results* contains the experimental results

## Tool
```
Usage: java -jar MutationAnalysisQNs.jar [-outFolder=<resultsFolder>]
       [-timeout=<testTimeoutMin>] [-operators=<operators>[,<operators>...]]...
       <modelPath>
      <modelPath>   path of the queieng network model in jsimg
      -operators=<operators>[,<operators>...]
                    selected mutation operators: CQSize, CNServers, CQStrat
      -outFolder=<resultsFolder>
                    path of folder for experimental results
      -timeout=<testTimeoutMin>
                    timeout in minutes
```

## People
* Thomas Laurent [https://csl.ucd.ie/index.php/thomas-laurent/](https://csl.ucd.ie/index.php/thomas-laurent/)
* Paolo Arcaini [https://group-mmm.org/~arcaini/](https://group-mmm.org/~arcaini/)
* Catia Trubiani [https://cs.gssi.it/catia.trubiani/](https://cs.gssi.it/catia.trubiani/)
* Anthony Ventresque [https://csl.ucd.ie/index.php/anthony-ventresque/](https://csl.ucd.ie/index.php/anthony-ventresque/)
