package fr.inria.edelweiss.kgraph.query;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.triple.parser.ASTQuery;
import fr.inria.acacia.corese.triple.parser.Constant;
import fr.inria.acacia.corese.triple.parser.Context;
import fr.inria.acacia.corese.triple.parser.Expression;
import fr.inria.acacia.corese.triple.parser.NSManager;
import fr.inria.acacia.corese.triple.parser.Term;
import fr.inria.edelweiss.kgram.api.core.Expr;
import fr.inria.edelweiss.kgram.api.core.ExprType;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.api.core.Pointerable;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Producer;
import fr.inria.edelweiss.kgram.core.Query;
import fr.inria.edelweiss.kgram.filter.Extension;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgtool.load.LoadException;
import fr.inria.edelweiss.kgtool.load.QueryLoad;
import fr.inria.edelweiss.kgtool.transform.TemplateVisitor;
import fr.inria.edelweiss.kgtool.transform.Transformer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Plugin to Transformer
 * 
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public class PluginTransform implements ExprType {
    static Logger logger = LogManager.getLogger(PluginTransform.class);

    protected IDatatype EMPTY = DatatypeMap.newStringBuilder("");
    private static final String VISIT_DEFAULT_NAME = NSManager.STL + "default";
    private static final IDatatype VISIT_DEFAULT = DatatypeMap.newResource(VISIT_DEFAULT_NAME);
    static final IDatatype TRUE = PluginImpl.TRUE;
    static final IDatatype FALSE = PluginImpl.FALSE;
    PluginImpl plugin;
    private int index = 0;

    PluginTransform(PluginImpl p) {
        plugin = p;
    }

    public Object function(Expr exp, Environment env, Producer p) {

        switch (exp.oper()) {
            
            case STL_NUMBER:
                return plugin.getValue(1 + env.count());

            case LEVEL:
                return getLevel(env, p);

            case STL_NL:
                return nl(null, env, p);

            case STL_ISSTART:
                return isStart(env, p);

            case STL_VISITED:
                return visited(exp, env, p);
                
           case STL_VISITED_GRAPH:
                return visitedGraph(exp, env, p);     

            case PROLOG:
                return prolog(null, env, p);

            case STL_PREFIX:
                return prefix(env, p);
                
            case XT_CONTEXT:
                return context(env, p);

            case STL_INDEX:
                return index(exp, env, p);

            case APPLY_TEMPLATES_ALL:
            case APPLY_TEMPLATES:
                return transform(null, null, null, exp, env, p);

            case FOCUS_NODE:
                return getFocusNode(null, env);
                
 

        }

        return null;
    }

    public Object function(Expr exp, Environment env, Producer p, IDatatype dt) {

        switch (exp.oper()) {


            case INDENT:
                return indent(dt, env, p);

            case STL_NL:
                return nl(dt, env, p);

            case PROLOG:
                return prolog(dt, env, p);

//            case STL_PROCESS:
//                return eval(exp, env, p, dt); 
                
            case STL_FUTURE:
                return dt;

            case STL_GET:
                return get(exp, env, p, dt);
                
            case STL_CGET:
                return cget(exp, env, p, dt);    
                
            case STL_SET:                
                return set(exp, env, p, dt, null);    

            case STL_BOOLEAN:
                return bool(exp, env, p, dt);

            case STL_VISITED:
                return visited(exp, env, p, dt);
                                                
            case STL_ERRORS:
                return errors(exp, env, p, dt);

            case STL_VISIT:
                return visit(exp, env, p, null, dt, null);

            case APPLY_TEMPLATES:
            case APPLY_TEMPLATES_ALL:
                return transformFocus(dt, null, null, exp, env, p);

            case CALL_TEMPLATE:
            case STL_TEMPLATE:
                return transform(null, dt, null, exp, env, p);

            case APPLY_TEMPLATES_WITH:
            case APPLY_TEMPLATES_WITH_ALL:
                return transform(dt, null, null, exp, env, p);

            case APPLY_TEMPLATES_GRAPH:
            case APPLY_TEMPLATES_NOGRAPH:
                return transform(null, null, dt, exp, env, p);

            case TURTLE:
                return turtle(dt, env, p);

            case PPURI:
            case URILITERAL:
            case XSDLITERAL:
                return uri(exp, dt, env, p);

            case STL_LOAD:
                load(dt, env, p);
                return EMPTY;


            case VISITED:
                return visited(dt, env, p);
                                
            case STL_FORMAT:
                return format(dt);     
                        

        }
        return null;
    }
    
    
        public Object function(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2) {
            switch(exp.oper()){
                
            case APPLY_TEMPLATES:
            case APPLY_TEMPLATES_ALL:
                // dt1: focus
                return transform(getArgs(dt1, dt2), dt1, null, null, null, exp, env, p);
                               
            case APPLY_TEMPLATES_WITH_GRAPH:
            case APPLY_TEMPLATES_WITH_NOGRAPH:
                // dt1: transformation 
                // dt2: graph            
                return transform(dt1, null, dt2, exp, env, p);

            case APPLY_TEMPLATES_WITH:
            case APPLY_TEMPLATES_WITH_ALL:
                // dt1: transformation
                // dt2: focus
                
               return transformFocus(dt2,  dt1, null, exp, env, p);

            case CALL_TEMPLATE:
            case STL_TEMPLATE:
                // dt1: template name
                // dt2: focus
                return transformFocus(dt2,  null, dt1, exp, env, p);

            case CALL_TEMPLATE_WITH:
                // dt1: transformation
                // dt2: template name
                return transform(dt1, dt2, null, exp, env, p);
                
              case TURTLE:
                return turtle(dt1, dt2, env, p);     
                
            case STL_SET:                
            case STL_EXPORT:
                return set(exp, env, p, dt1, dt2);
                
            case STL_VGET:
                return vget(exp, env, p, dt1, dt2);    
                
           case STL_VISIT:
                return visit(exp, env, p, dt1, dt2, null);
                
            case STL_GET:
                return get(exp, env, p, dt1, dt2);
                
            case STL_CGET:
                return cget(exp, env, p, dt1, dt2);   
                                   
            case STL_FORMAT:
                return format(dt1, dt2);
                 
    
        }
            return null;
        }
    
    
    
    public Object eval(Expr exp, Environment env, Producer p, Object[] args) {
        IDatatype[] param = (IDatatype[]) args;
        switch (exp.oper()){
            
           case STL_PROCESS:
                return processDef(exp, env, p, param);     
        }

        IDatatype dt1 =  param[0];
        IDatatype dt2 =  param[1];
        IDatatype dt3 =  param[2];

        switch (exp.oper()) {

         
            case CALL_TEMPLATE:
            case STL_TEMPLATE:
                // dt1: template name
                // dt2: focus
                return transform(getArgs(param, 1), dt2,  null, dt1, null, exp, env, p);

            case CALL_TEMPLATE_WITH:
                // dt1: transformation
                // dt2: template name
                // dt3: focus
                return transform(getArgs(param, 2), dt3,  dt1, dt2, null, exp, env, p);

            case APPLY_TEMPLATES_WITH:
            case APPLY_TEMPLATES_WITH_ALL:
                // dt1: transformation
                // dt2: focus
                
               return transform(getArgs(param, 1), dt2,  dt1, null, null, exp, env, p);
                
            case APPLY_TEMPLATES:
            case APPLY_TEMPLATES_ALL:
                // dt1: focus
                return transform(getArgs(param, 0), dt1,  null, null, null, exp, env, p);  
                
            case APPLY_TEMPLATES_WITH_GRAPH:
            case APPLY_TEMPLATES_WITH_NOGRAPH:
                // dt1: transformation 
                // dt2: graph 
                // dt3; focus
                return transform(getArgs(param, 2), dt3,  dt1, null, dt2, exp, env, p);
                
            case STL_VISIT:
                return visit(exp, env, p, dt1, dt2, dt3);
                
            case STL_VSET:
                return vset(exp, env, p, dt1, dt2, dt3); 
                
            case STL_CSET:
                return cset(exp, env, p, dt1, dt2, dt3);     
                
            case STL_FORMAT:
                return format(param);
                           
        }

        return null;
    }
    
    
    
    
    IDatatype[] getArgs(IDatatype[] obj, int n){
        return Arrays.copyOfRange(obj, n, obj.length);
    }
    
    IDatatype[] getArgs(IDatatype dt1, IDatatype dt2){
        IDatatype arr[] = new IDatatype[2];
        arr[0] = dt1;
        arr[1] = dt2;
        return arr;
    }
    
    String getFormat(IDatatype dt){
        if (dt.isURI()){
            return getFormatURI(dt.stringValue());
        }
        return dt.stringValue();
    }
    
    String getFormatURI(String uri){
        QueryLoad ql = QueryLoad.create();
        try {
            return ql.readWE(uri);
        } catch (LoadException ex) {
            java.util.logging.Logger.getLogger(PluginTransform.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
          
    IDatatype format(IDatatype dt){
        return plugin.getValue(getFormat(dt));
    }
    
    IDatatype format(IDatatype dt1, IDatatype dt2){
        return plugin.getValue(String.format(getFormat(dt1), dt2.stringValue()));
    }
    
    IDatatype format(IDatatype[] par){
        String f = getFormat(par[0]);
        switch (par.length){
            case 2: return plugin.getValue(String.format(f, par[1].stringValue()));
            case 3: return plugin.getValue(String.format(f, par[1].stringValue(), par[2].stringValue()));
            case 4: return plugin.getValue(String.format(f, par[1].stringValue(), par[2].stringValue(), par[3].stringValue()));
            case 5: 
                return plugin.getValue(String.format(f, par[1].stringValue(), par[2].stringValue(), 
                        par[3].stringValue(), par[4].stringValue()));
            case 6: 
                return plugin.getValue(String.format(f, par[1].stringValue(), par[2].stringValue(), 
                        par[3].stringValue(), par[4].stringValue(), par[5].stringValue()));                                   
            case 7: 
                return plugin.getValue(String.format(f, par[1].stringValue(), par[2].stringValue(), 
                        par[3].stringValue(), par[4].stringValue(), par[5].stringValue(),
                        par[6].stringValue()));                                   
            case 8: 
                return plugin.getValue(String.format(f, par[1].stringValue(), par[2].stringValue(), 
                        par[3].stringValue(), par[4].stringValue(), par[5].stringValue(),
                        par[6].stringValue(), par[7].stringValue())); 
            case 9: 
                return plugin.getValue(String.format(f, par[1].stringValue(), par[2].stringValue(), 
                        par[3].stringValue(), par[4].stringValue(), par[5].stringValue(),
                        par[6].stringValue(), par[7].stringValue(), par[8].stringValue()));

            case 10: 
                return plugin.getValue(String.format(f, par[1].stringValue(), par[2].stringValue(), 
                        par[3].stringValue(), par[4].stringValue(), par[5].stringValue(),
                        par[6].stringValue(), par[7].stringValue(), par[8].stringValue(), par[9].stringValue()));        
            case 11: 
                return plugin.getValue(String.format(f, par[1].stringValue(), par[2].stringValue(), 
                        par[3].stringValue(), par[4].stringValue(), par[5].stringValue(),
                        par[6].stringValue(), par[7].stringValue(), par[8].stringValue(), 
                        par[9].stringValue(), par[10].stringValue())); 
        
        }
        return null;
    }
    

    private IDatatype bool(Expr exp, Environment env, Producer p, IDatatype dt) {
        if (dt.stringValue().contains("false")) {
            return FALSE;
        }
        return TRUE;
    }

    /**
     * Return or create current transformer (create in case of different graph)
     */
    Transformer getTransformer(Environment env, Producer p) {
        return getTransformer(null, env, p, (IDatatype) null, (IDatatype) null, null);
    }
    
    /**
     * Return current transformer (do not create in case of different graph)
     */
    Transformer getTransformerCurrent(Environment env, Producer p) {
        return getTransformer(null, env, p, (IDatatype) null, (IDatatype) null, null, true);
    }
    
    Transformer getTransformer(Expr exp, Environment env, Producer prod, IDatatype uri, IDatatype temp, IDatatype dtgname) {
        return getTransformer(exp, env, prod, uri, temp, dtgname, false);
    }
    
    /**
     * uri: transformation URI 
     * gname: named graph 
     * If uri == null, get current transformer
     * current = true: return current transformer even if not same graph
     * use case: graph ?shape {  st:cget(sh:def, ?name)  }
     * TODO: cache for named graph
     */
    Transformer getTransformer(Expr exp, Environment env, Producer prod, IDatatype uri, IDatatype temp, IDatatype dtgname, boolean current) {
        Query q = env.getQuery();
        ASTQuery ast = (ASTQuery) q.getAST();
        String transform = getTrans(uri, temp);
        Transformer t = (Transformer) q.getTransformer(transform);

        if (transform == null && t != null) {
            transform = t.getTransformation();
        }

        if (dtgname != null) {
            // transform named graph
            if (dtgname.isPointer() && dtgname.pointerType() == Pointerable.GRAPH_POINTER){
                // dtgname contains a Graph
                // use case: let (?g = construct {} where {}){ 
                // st:apply-templates-with-graph(st:navlab, ?g) }
                t = Transformer.create((Graph)dtgname.getPointerObject(), transform);
                complete(q, t, uri);
            }
            else {
                String gname = dtgname.getLabel();
                try {              
                    t = Transformer.create((Graph) prod.getGraph(), transform, gname, isWith(exp));
                    complete(q, t, uri);
                } catch (LoadException ex) {
                    logger.error(ex);
                    t = Transformer.create(Graph.create(), null);
                }
            }
        } else if (t == null) {
            t = Transformer.create(prod, transform);
            complete(q, t, uri);
            q.setTransformer(transform, t);
        } else if (! current){
            Graph g = t.getGraph();
            if (g != prod.getGraph()) {
                // Transformer exist but with another graph
                // create a new one
                t = Transformer.create(prod, transform);
                complete(q, t, uri);
            }
        }

        return t;
    }
    
    boolean isWith(Expr exp){
        return (exp == null) ? true
                        : exp.oper() == ExprType.APPLY_TEMPLATES_GRAPH
                        || exp.oper() == ExprType.APPLY_TEMPLATES_WITH_GRAPH;
    }
    

    void complete(Query q, Transformer t, IDatatype uri) {
        t.complete(q, (Transformer) q.getTransformer());
        if (uri != null){
            t.getContext().set(Transformer.STL_TRANSFORM, uri);
        }
    }

    /**
     * Increment indentation level
     */
    IDatatype indent(IDatatype dt, Environment env, Producer prod) {
        Transformer t = getTransformer(env, prod);
        t.setLevel(t.getLevel() + dt.intValue());
        return EMPTY;
    }

    /**
     * New Line with indentation given by t.getLevel() Increment level if
     * dt!=null
     */
    IDatatype nl(IDatatype dt, Environment env, Producer prod) {
        Transformer t = getTransformer(env, prod);
        if (dt != null) {
            t.setLevel(t.getLevel() + dt.intValue());
        }
        return t.tabulate();
    }

    IDatatype prolog(IDatatype dt, Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        String title = null;
        if (dt != null) {
            title = dt.getLabel();
        }
        String pref = p.getNSM().toString(title);
        return plugin.getValue(pref);
    }

    IDatatype prefix(Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        return DatatypeMap.createObject(p.getNSM());
    }
    
    IDatatype context(Environment env, Producer prod) {
        return DatatypeMap.createObject(getContext(env, prod));
    }
    
    IDatatype isStart(Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        boolean b = p.isStart();
        return plugin.getValue(b);
    }
    
    String getLabel(IDatatype dt) {
        if (dt == null) {
            return null;
        }
        return dt.getLabel();
    }
    
    String getTransform(IDatatype trans, IDatatype temp){
        if (trans == null && temp == null){
             return null;
        }       
        IDatatype dt = (trans != null) ? trans : temp;
        return Transformer.getURI(dt.getLabel());            
    }
    
    String getTemplate(IDatatype trans, IDatatype temp){
        if (temp != null){
            return getLabel(temp);
        }
        if (trans != null){
            return Transformer.getStartName(trans.getLabel());
        }
        return null;
    }
    
    String getTrans(IDatatype trans, IDatatype temp){
       return getLabel(trans);    
    }
    
    String getTemp(IDatatype trans, IDatatype temp){
        return getLabel(temp);
    }

    /**
     * Without focus node
     */
    IDatatype transform(IDatatype trans, IDatatype temp, IDatatype name, Expr exp, Environment env, Producer prod) {
        Transformer p = getTransformer(exp, env, prod, trans, temp, name);
        return p.process(getTemp(trans, temp),
                exp.oper() == ExprType.APPLY_TEMPLATES_ALL
                || exp.oper() == ExprType.APPLY_TEMPLATES_WITH_ALL,
                exp.getModality());
    }

   
    IDatatype transformFocus(IDatatype focus,  IDatatype trans, IDatatype temp, Expr exp, Environment env, Producer prod) {
        return transform(null, focus,  trans, temp, null, exp, env, prod);
    }

    /**
     * exp:   calling expression eg st:apply-templates
     * focus: focus node, arg is an argument node
     * trans: transformation URI, may be null
     * temp:  name of a named template, may be null
     * name:  named graph
     */    
    IDatatype transform(IDatatype[] args, IDatatype focus, IDatatype trans, IDatatype temp, IDatatype name,
            Expr exp, Environment env, Producer prod) {
        Transformer p = getTransformer(exp, env, prod, trans, temp, name);
        IDatatype dt = p.process(args, focus, 
                getTemp(trans, temp),
                exp.oper() == ExprType.APPLY_TEMPLATES_ALL
                || exp.oper() == ExprType.APPLY_TEMPLATES_WITH_ALL,
                exp.getModality(), exp, env.getQuery());
        return dt;
    }

    /**
     * st:process(var) : default variable processing by SPARQL Template Ask
     * Transformer what is default behavior set st:process() to it's default
     * behavior the default behavior is st:turtle
     */
    public Object processDef(Expr exp, Environment env, Producer p, IDatatype[] args) {
        Extension ext = env.getQuery().getExtension();
        if (ext != null && ext.isDefined(exp)) {
            return plugin.getEvaluator().eval(exp, env, p, args, ext);
        }

        Transformer pp = getTransformer(env, p);
        int oper = pp.getProcess();

        // overload current st:process() oper code to default behaviour oper code
        // future executions of this st:process() will directly execute target default behavior
        exp.setOper(oper);
        Object res = plugin.function(exp, env, p,  args[0]);
        return res;

    }
  
 
    /**
     * exp = st:aggregate(?out)
     * Overload exp with actual transformer aggregate
     * May be defined in template st:profile 
     * using function st:aggregate(?x) { st:agg_and(?x) }
     * otherwise, default is st:group_concat
     */
     Expr decode(Expr exp, Environment env, Producer p){
        Query q = env.getQuery();
        Extension ext = q.getExtension();
        Transformer t = getTransformer(env, p);
        
        switch (exp.oper()){
             case STL_AGGREGATE:
                 // possibly defined in template st:profile
                 Expr def = null;
                 if (ext != null){
                     def = ext.get(exp);
                 }
                 // default aggregate
                 int oper = t.getAggregate();
                 exp = decode(exp, def, oper);
         }
        
         return exp;
     }
     
     
     /**
      * def = st:aggregate(?x) = st:agg_and(?x)
      * Overload exp operator with current transformer operator
      */
     Expr decode(Expr exp, Expr def, int oper){
         if (def != null){
             oper = def.getBody().oper(); 
         }
         exp.setOper(oper);
         return exp;        
     }
    
     /**
      * Context of current Transformer
      */
   Context getTransformerContext(Environment env, Producer p){
       Transformer t = getTransformerCurrent(env, p);
       return t.getContext();
   }
   
   Context getQueryContext(Environment env, Producer p) {
        Query q = env.getQuery().getGlobalQuery();
        Context c = (Context) q.getContext(); 
        if (c == null && ! q.isTransformationTemplate()){
            //  std Query or Template alone
            c = new Context();
            q.setContext(c);
        }
        return c;
   }
 
   /**
    * Transformation templates share Transformer Context
    * Query and Template alone have own Context
    */
    Context getContext(Environment env, Producer p) {
        Context c = getQueryContext(env, p);
        if (c == null){
            c = getTransformerContext(env, p);
            env.getQuery().setContext(c);
        }
        return c;
    }
          
    public IDatatype get(Expr exp, Environment env, Producer p, IDatatype dt) {
        return get(exp, env, p, dt.getLabel());
    }

    public IDatatype get(Expr exp, Environment env, Producer p, String name) {
        return getContext(env, p).get(name);
    }

    public IDatatype get(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2) {
        IDatatype dt = get(exp, env, p, dt1);
        if (dt == null) {
            return FALSE;
        }
        boolean b = dt.equals(dt2);
        return plugin.getValue(b);
    }
    
    public IDatatype cget(Expr exp, Environment env, Producer p, IDatatype name) {
        return getContext(env, p).getContext(name).getDatatypeValue();
    }
    
    public IDatatype cget(Expr exp, Environment env, Producer p, IDatatype name, IDatatype slot) {
        return getContext(env, p).cget(name, slot);
    }

    public IDatatype cset(Expr exp, Environment env, Producer p, IDatatype name, IDatatype slot, IDatatype value) {
        getContext(env, p).cset(name, slot, value);
        return value;
    }

    public IDatatype index(Expr exp, Environment env, Producer p) {
        return plugin.getValue(index++);
    }

    /**
     * st:set(st:value, st:test)
     * st:export(st:value, st:test)
     * Set/export context property value
     * Exported is transmitted to query Transformer Context if any
     * that is when query executes st:apply-templates-with(st:turtle)
     * the turtle transformer inherits the exported context property value
     * Special case with server: query and transformer share *same* context
     * hence in server mode, when query st:set(), the property is transmitted to next Transformer
     * use case: profile with query + transformation, q and t share *same* Context
     * 
     */
     public Object set(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2) {
        Context c = getContext(env, p);
        if (exp.oper() == STL_SET){
          c.set(dt1.getLabel(), dt2);
        }
        else {
           c.export(dt1.getLabel(), dt2); 
        }
        return dt2;
    }  
    
     
    TemplateVisitor getVisitor(Environment env, Producer p){
        TemplateVisitor tv = (TemplateVisitor) env.getQuery().getTemplateVisitor();
        if (tv == null){
            tv = getTransformer(env, p).defVisitor();
            env.getQuery().setTemplateVisitor(tv);
        }
        return tv;
    }

    public IDatatype vset(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2, IDatatype dt3) {
        return getVisitor(env, p).set(dt1, dt2, dt3);
    }

    public IDatatype vget(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2) {
        return getVisitor(env, p).get(dt1, dt2);
    }

    public IDatatype visited(Expr exp, Environment env, Producer p) {
        Collection<IDatatype> list = getVisitor(env, p).visited();
        return DatatypeMap.createList(list);
    }
    
    public IDatatype visitedGraph(Expr exp, Environment env, Producer p) {
        return getVisitor(env, p).visitedGraphNode();
    }

    public IDatatype visited(Expr exp, Environment env, Producer p, IDatatype dt) {
        boolean b = getVisitor(env, p).isVisited(dt);
        return plugin.getValue(b);
    }
    
     public IDatatype errors(Expr exp, Environment env, Producer p, IDatatype dt) {
        Collection<IDatatype> list = getVisitor(env, p).getErrors(dt);
        return DatatypeMap.createList(list);
    }

    // Visitor design pattern
    public IDatatype visit(Expr exp, Environment env, Producer p, IDatatype dt1, IDatatype dt2, IDatatype dt3) {        
        if (dt1 == null) {
            dt1 = VISIT_DEFAULT;
        }
        getVisitor(env, p).visit(dt1, dt2, dt3);
        return TRUE;
    }

    
    
    
    // stack visit
    IDatatype visited(IDatatype dt, Environment env, Producer p) {      
        Transformer t = getTransformer(env, p);
        boolean b = t.isVisited(dt);
        return plugin.getValue(b);
    }
    
     /**
     * create concat(str, st:number(), str)
     */
    public Expr createFunction(String name, List<Object> args, Environment env){
        Term t = Term.function(name);
        for (Object arg : args){
            if (arg instanceof IDatatype){
                // str: arg is a StringBuilder, keep it as is
                Constant cst = Constant.create("Future", null, null);
                cst.setDatatypeValue((IDatatype) arg);
                t.add(cst);
            }
            else {
                // st:number()
               t.add((Expression) arg);
            }
        }
        t.compile((ASTQuery)env.getQuery().getAST());
        return t;
    }
    

    /**
     *
     *
     */
    IDatatype turtle(IDatatype o, IDatatype o2, Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        IDatatype dt = p.turtle(o, o2.equals(TRUE));
        return dt;
    }

    IDatatype turtle(IDatatype o, Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        IDatatype dt = p.turtle(o);
        return dt;
    }

    IDatatype xsdLiteral(IDatatype o, Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        IDatatype dt = p.xsdLiteral(o);
        return dt;
    }
    
    /**
     * @deprecated
     */
    IDatatype uri(Expr exp, IDatatype dt, Environment env, Producer prod) {
        if (dt.isURI()) {
            return turtle(dt, env, prod);
        } else if (dt.isLiteral() && exp.oper() == ExprType.URILITERAL) {
            return turtle(dt, env, prod);
        } else if (dt.isLiteral() && exp.oper() == ExprType.XSDLITERAL) {
            return xsdLiteral(dt, env, prod);
        } else {
            return transformFocus(dt,  null, null, exp, env, prod);
        }
    }

    IDatatype getLevel(Environment env, Producer prod) {
        return plugin.getValue(level(env, prod));
    }

    int level(Environment env, Producer prod) {
        Transformer p = getTransformer(env, prod);
        return p.level();
    }

    void load(IDatatype dt, Environment env, Producer p) {
        Transformer t = getTransformer(env, p);
        t.load(dt.getLabel());
    }

    private Object getFocusNode(IDatatype dt, Environment env) {
        String name = Transformer.IN;
        if (dt != null) {
            name = dt.getLabel();
        }
        Node node = env.getNode(name);
        if (node == null) {
            return null;
        }
        return node.getValue();
    }
     
}
