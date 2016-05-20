package fr.inria.corese.kgtool.workflow;

import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.exceptions.EngineException;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.core.GraphStore;
import fr.inria.edelweiss.kgtool.load.LoadException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic Process to be programmed
 * Select behavior with sw:mode attribute
 * Use cases: 
 * Produce a structured dataset from parallel process result graphs
 * Compare two graphs
 * 
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 *
 */
public class DatasetProcess extends WorkflowProcess {
    
    DatasetProcess(){}
    
    DatasetProcess(String mode){
        setMode(DatatypeMap.newResource(mode));
    }
    
    @Override
    void start(Data data){
    }
    
    @Override
    void finish(Data data){
        
    }
    
    @Override
    public Data run(Data data) {
        if (getMode() == null) {
            return dataset(data);
        }
        String mode = getMode().getLabel();
        if (mode.equals(WorkflowParser.WORKFLOW_VALUE)){
            return workflow(data);
        }
        else if (mode.equals(WorkflowParser.COMPARE)){
            return compare(data);
        }
        else if (mode.equals(WorkflowParser.VISITOR)){
            Data res = new Data(this, data.getVisitor().visitedGraph());
            res.setVisitor(data.getVisitor());
            return res;
        }
        return data;
    }
    
    /**
     * sw:workflow named graph is a workflow description
     * parse and run workflow on data graph
     */
    Data workflow(Data data) {
        Data tmp = dataset(data);
        Graph wg = tmp.getGraph().getNamedGraph(WorkflowParser.WORKFLOW_VALUE);
        Graph g  = tmp.getGraph();
        if (wg == null){
            wg = data.getGraph();
            g = GraphStore.create();
        }
        Data input = data.copy();
        input.setGraph(g);
        
        try {
            SemanticWorkflow sw = new WorkflowParser().parse(wg);
            Data res = sw.process(input);
            return res;
        } catch (LoadException ex) {
            Logger.getLogger(DatasetProcess.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EngineException ex) {
            Logger.getLogger(DatasetProcess.class.getName()).log(Level.SEVERE, null, ex);
        }

        return data;
    }
    
    /**
     * Create a structured Dataset from a list of Data Graph Pick a main Graph,
     * set other graphs as named graphs
     * Process of Parallel may have a name which means that it is a named graph
     */
    Data dataset(Data data) {
         if (data.getDataList() != null) {
            // pick main Graph
            Data res = data.getResult();
            if (res != null) {
                Graph g = res.getGraph();
                for (Data dd : data.getDataList()) {                   
                    if (dd.getName() != null && dd.getGraph() != null && dd != res) {                        
                        namedGraph(g, dd);
                    }
                }
                Data value = new Data(this, g);
                return value;
            }
        }
        return data;
    }
    
    void namedGraph(Graph g, Data dd){
        dd.getGraph().init();
        // declare dd as extended named graph
        g.setNamedGraph(dd.getName(), dd.getGraph()); 
        // export dd in context using sw:name as context property
        getCreateContext().export(dd.getName(), DatatypeMap.createObject(dd.getGraph()));
    }
    
    Data compare(Data data) {
        if (data.getDataList() != null && data.getDataList().size() >= 2) {
            Graph g1 = data.getDataList().get(0).getGraph();
            Graph g2 = data.getDataList().get(1).getGraph();
            boolean b = g1.compare(g2, true, true, isDebug());
            data.setSuccess(b);
            data.setDatatypeValue(DatatypeMap.newInstance(b));
        }
        return data;
    }

}
