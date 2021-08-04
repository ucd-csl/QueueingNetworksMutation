import mutation.Mutation;
import mutation.MutationContext;
import mutationoperators.*;
import mutationtesting.MutationResult;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import picocli.CommandLine;
import qn.QueueNetwork;
import qn.QueueNetworkParser;
import qn.simulation.MeasureResult;
import qn.simulation.Simulation;
import qn.simulation.SimulationResults;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Formatter;
import java.util.logging.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "java -jar MutationAnalysisQNs.jar", version = "MutationAnalysisQNs 1.0", mixinStandardHelpOptions = false)
public class QnMutationAnalysis implements Runnable {
    private static final Logger logger = Logger.getLogger("");

    @CommandLine.Parameters(index = "0", description = "path of the queieng network model in jsimg")
    String modelPath;

    @CommandLine.Option(names = "-operators", description = "selected mutation operators: CQSize, CNServers, CQStrat", split = ",")
    MUTATION_OPERATORS[] operators = MUTATION_OPERATORS.values();

    @CommandLine.Option(names = "-outFolder", description = "path of folder for experimental results", defaultValue = "./results")
    String resultsFolder;

    @CommandLine.Option(names = "-timeout", description = "timeout in minutes", defaultValue = "5")
    int testTimeoutMin;

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new QnMutationAnalysis()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        setup();
        try {
            logger.info("----------------------\nRunning the test on the original network " + modelPath);
            Path fullPath = Paths.get(modelPath).toAbsolutePath();
            modelPath = fullPath.toString();
            String modelFolder = fullPath.getParent().toString();
            XMLOutputter out = new XMLOutputter();
            out.setFormat(Format.getPrettyFormat());
            SAXBuilder builder = new SAXBuilder();
            Document document = null;
            document = builder.build(modelPath);
            Simulation.runSimulation(document, modelFolder);
            SimulationResults originalResult = Simulation.readSimulationResults(modelFolder + "/" + document.getRootElement().getAttributeValue("name"));
            logger.info("-----------\n\n");


            QueueNetwork qn = QueueNetworkParser.parse(modelPath);
            MutationContext context = new MutationContext();
            CompoundOperator co = new CompoundOperator(context);

            List<MUTATION_OPERATORS> listOperators = Arrays.stream(operators).collect(Collectors.toList());
            if (listOperators.contains(MUTATION_OPERATORS.CNServers)) {
                co.addOperator(new ChangeNumberServers(context));
                co.addOperator(new ChangeNumberServersM1(context));
            }
            if (listOperators.contains(MUTATION_OPERATORS.CQSize)) {
                co.addOperator(new ChangeQueueSize(context));
                co.addOperator(new ChangeQueueSizeM1(context));
            }
            if (listOperators.contains(MUTATION_OPERATORS.CQStrat)) {
                co.addOperator(new ChangeQueueingStrategy(context));
            }
            co.identifyMutants(qn);
            Collection<Mutation> mutations = co.getContext().getMutations();

            int counter = 0;
            String prefix = Paths.get(modelPath).getFileName().toString().replaceAll(".jsimg", "");
            Collection<MutationResult> mutationResults = new ArrayList<>();
            Path mutantsFolderPath = Paths.get(resultsFolder, "mutants");
            Files.createDirectories(mutantsFolderPath);
            String mutantsFolderStr = mutantsFolderPath.toString();
            for (Mutation m : mutations) {
                counter++;

                logger.info("----------------------\nRunning mutant: " + m.getDescription() + " at " + m.getLocation());
                String fileNameNoExtension = prefix + "_" + m.getOperator() + "_" + counter;
                String prefixMutant = mutantsFolderStr + "/" + fileNameNoExtension;
                String fullPathMutant = prefixMutant + ".jsimg";
                String fileNameWithExtension = fileNameNoExtension + ".jsimg";
                Document mutant = co.generateMutant(qn, m);
                assert mutant != null : fullPathMutant;
                setNameInDocument(mutant, fileNameWithExtension);

                out.output(mutant, new FileWriter(fullPathMutant));

                ExecutorService service = Executors.newSingleThreadExecutor();
                Document mutantWithTest = mutant;
                long testTime = System.currentTimeMillis();
                Callable<Boolean> simulationCallable = createSimulationCallable(mutantsFolderStr, mutantWithTest);
                boolean error = false;
                Future<Boolean> futureTask = service.submit(simulationCallable);
                try {
                    long timeoutForMutant = testTimeoutMin;
                    error = !futureTask.get(timeoutForMutant, TimeUnit.MINUTES);
                    testTime = (System.currentTimeMillis() - testTime);
                } catch (TimeoutException timeoutException) {
                    testTime = (System.currentTimeMillis() - testTime);
                    futureTask.cancel(true);
                    logger.info("Mutant " + m.getDescription() + " was KILLED by timeout. Test time " + (testTime / 60000.0) + " mins.");
                    mutationResults.add(new MutationResult(m, "test", true));
                    //break;//We should not break. We still want to evaluate all the tests
                    continue;
                } catch (InterruptedException | ExecutionException e) {
                    testTime = (System.currentTimeMillis() - testTime);
                    logger.info("Mutant " + m.getDescription() + " was KILLED by error. Test time " + (testTime / 60000.0) + " mins.");
                    mutationResults.add(new MutationResult(m, "test", false));
                    continue;
                } finally {
                    service.shutdownNow();
                }

                if (error) {
                    //killed
                    logger.info("Mutant " + m.getDescription() + " was KILLED by error. Test time " + (testTime / 60000.0) + " mins.");
                    mutationResults.add(new MutationResult(m, "test", false));
                    continue;
                }
                SimulationResults resultMutant = Simulation.readSimulationResults(fullPathMutant);
                logger.info("Mutant " + m.getDescription() + " not killed by error nor by timeout. Test time " + (testTime / 60000.0) + " mins.");
                mutationResults.add(new MutationResult(m, originalResult, resultMutant, "test"));
                logger.info("----------------------\n");
            }
            saveResults(modelPath, mutationResults);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setNameInDocument(Document doc, String name) {
        Element rootElement = doc.getRootElement();
        rootElement.setAttribute("name", name);
        rootElement.getChild("sim").setAttribute("name", name);
    }

    private static Callable<Boolean> createSimulationCallable(String folder, Document document) {
        Callable<Boolean> simulationTask = () -> {
            try {
                Simulation.runSimulation(document, folder);
            } catch (Exception e) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        };
        return simulationTask;
    }

    private void saveResults(String modelName, Collection<MutationResult> mutationResults) throws IOException {
        File outputDir = new File(resultsFolder);
        if ((outputDir.exists() && !outputDir.isDirectory()) || (!outputDir.exists() && !outputDir.mkdirs())) {
            throw new IOException("Could not make output directory");
        }
        String prefix = Paths.get(modelName).getFileName().toString().replaceAll(".jsimg", "");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String resultsTime = simpleDateFormat.format(new GregorianCalendar(Locale.getDefault()).getTime());
        try (FileWriter fw = new FileWriter(new File(resultsFolder + File.separator + prefix + "_results_" + resultsTime + ".tsv"));
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("test\tlocation\toperator\t" + MeasureResult.printHeaderFixedPart("") + "\t" + MeasureResult.printHeaderVariablePart("orig_") + "\t" + MeasureResult.printHeaderVariablePart("mut_") + "\ttimeoutMut\n");
            for (MutationResult result : mutationResults) {
                bw.write(result.printMetrics());
            }
        }
    }

    public static void setup() {
        // suppress the logging output to the console
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers[0] instanceof ConsoleHandler) {
            rootLogger.removeHandler(handlers[0]);
        }

        Arrays.stream(logger.getHandlers()).forEach(handler -> logger.removeHandler(handler));
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getMessage() + "\n";
            }
        });
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.INFO);
    }
}

enum MUTATION_OPERATORS {
    CQSize, CNServers, CQStrat
}
