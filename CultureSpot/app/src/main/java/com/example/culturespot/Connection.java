package com.example.culturespot;

import android.os.AsyncTask;
import android.util.Pair;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Connection extends AsyncTask<String, Void, HashMap<Value,HashMap<String, Value>>> {
    private MapFragment mapFragment;
    private HTTPRepository repository;
    String url;
    private RepositoryConnection connection;
    private String flag;
    private boolean wiki;
    public Connection(String url,MapFragment mapFragment) {
        this.mapFragment = mapFragment;
        this.url=url;
        if(url.contains("google")) this.repository=null;
        else this.repository = new HTTPRepository(url);
        this.connection = null;
    }
    public String getNames(ArrayList<String> names) {
        String s = "";
        boolean flag=false;
        for (int i = 0; i < names.size(); i++) {
            if (flag) s += ",";
            else flag=true;
            s += "\""+names.get(i)+"\"";
        } // get all names for query
        return s;
    }
    //code from GRAPHDB's site
    protected HashMap<Value,HashMap<String, Value>> doInBackground(String... args) {
        HashMap<String,Value> nameIds=new HashMap<String, Value>();
        HashMap<Value,HashMap<String, Value>> results = new HashMap<Value,HashMap<String, Value>>();
        ArrayList<String> names=new ArrayList<>();
        this.flag=args[1];
        if(this.flag.equals("directions")) { // execute http get to google maps for directions
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(url);
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                Value v=null;
                   HashMap<String, Value> hashMap=new HashMap<String, Value>();
                   hashMap.put(EntityUtils.toString(response.getEntity()),v);
                   results.put(v,hashMap);
                   return results;
            }
            catch (ClientProtocolException e) {
            }
            catch (IOException e) {
            }
        }
        else{
            try {
                connection = repository.getConnection();
            }
            catch (RepositoryException e) {
                e.printStackTrace();
            }
            try { //graphdb query
                // Preparing a SELECT query for later evaluation
                TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL,args[0]);
                // Evaluating a prepared query returns an iterator-like object
                // that can be traversed with the methods hasNext() and next()
                TupleQueryResult tupleQueryResult = tupleQuery.evaluate();

                while (tupleQueryResult.hasNext()) {
                    // Each result is represented by a BindingSet, which corresponds to a result row
                    BindingSet bindingSet = tupleQueryResult.next();
                    // Each BindingSet contains one or more Bindings
                    HashMap<String, Value> hashMap=new HashMap<String, Value>();
                    Value id=null;
                    String name1=null;
                    wiki=false;
                    for (Binding binding : bindingSet) {
                        // Each Binding contains the variable name and the value for this result row
                        String name = binding.getName();
                        Value value =  binding.getValue();
                        if(flag.equals("autocomplete")) results.put(value, hashMap); // we only need names
                        //regular query for all information
                        else if(name.equals("id")){
                            results.put(value, hashMap);
                            id=value;
                        }
                        else {
                            if(name.equals("wiki")) wiki=true;
                            if(name.equals("name")){
                                name1 = value.toString();
                                name1 = name1.substring(name1.indexOf("\"") + 1, name1.lastIndexOf("\""));
                                name1=name1.toLowerCase();
                                while(name1.endsWith(" "))  name1 = name1.substring(0, name1.length() - 1);
                                while(name1.startsWith(" ")) name1 = name1.substring(1, name1.length());

                            }
                            hashMap.put(name,value);
                        }
                    }
                    if(!wiki){ // add to list to find wiki page from DBpedia
                        names.add(name1);
                        nameIds.put(name1,id);
                    }
                    // Bindings can also be accessed explicitly by variable name
                    //Binding binding = bindingSet.getBinding("x");
                }

                // Once we are done with a particular result we need to close it
                tupleQueryResult.close();


                // Doing more with the same connection object
                // ...
            } catch (
                    MalformedQueryException e) {
                e.printStackTrace();
            } catch (
                    RepositoryException e) {
                e.printStackTrace();
            } catch (
                    QueryEvaluationException e) {
                e.printStackTrace();
            } finally {
                // It is best to close the connection in a finally block
                try {
                    connection.close();
                    if(!flag.equals("autocomplete")){
                        this.repository = new HTTPRepository( "https://dbpedia.org/sparql");
                        try {
                            connection = repository.getConnection();
                        }
                        catch (RepositoryException e) {
                            e.printStackTrace();
                        }
                        try { //query DBpedia for missing wiki pages
                            // Preparing a SELECT query for later evaluation
                            TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL,
                                    "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                                    +"SELECT DISTINCT ?wiki ?name\n"
                                    +"WHERE {\n"
                                    +"?place foaf:isPrimaryTopicOf ?wiki.\n"
                                    +"?place foaf:name ?name.\n"
                                    +"    FILTER (lcase(str(?name)) IN (" + getNames(names) + "))\n"
                                    +"}\n");
                            // Evaluating a prepared query returns an iterator-like object
                            // that can be traversed with the methods hasNext() and next()
                            TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
                            ArrayList<Pair<String,Value>> namesWiki=new ArrayList<Pair<String, Value>>();
                            while (tupleQueryResult.hasNext()) {
                                // Each result is represented by a BindingSet, which corresponds to a result row
                                BindingSet bindingSet = tupleQueryResult.next();
                                // Each BindingSet contains one or more Bindings
                                String n=null;
                                Value w=null;
                                for (Binding binding : bindingSet) {
                                    // Each Binding contains the variable name and the value for this result row
                                    String name = binding.getName();
                                    Value value =  binding.getValue();
                                    if(name.equals("name"))  n=value.toString().substring(0,value.toString().length()-3).replace("\"","");
                                    else w=value;
                                }
                                Pair<String,Value> p=new Pair<String,Value>(n,w);
                                namesWiki.add(p);
                            }
                            // Once we are done with a particular result we need to close it
                            tupleQueryResult.close();
                            names.clear();
                            for(Pair<String,Value> p : namesWiki){ // add missing wiki pages to results
                                Value id1=nameIds.get(p.first.toLowerCase());
                                if(names.contains(p.first.toLowerCase())) continue;
                                else names.add(p.first.toLowerCase());
                                HashMap<String,Value> h=new HashMap<>(results.get(id1));
                                results.remove(id1);
                                h.put("wiki",p.second);
                                results.put(id1,h);
                            }
                            // Doing more with the same connection object
                            // ...
                        } catch (
                                MalformedQueryException e) {
                            e.printStackTrace();
                        } catch (
                                RepositoryException e) {
                            e.printStackTrace();
                        } catch (
                                QueryEvaluationException e) {
                            e.printStackTrace();
                        } finally {
                            // It is best to close the connection in a finally block
                            try {
                                connection.close();
                            } catch (RepositoryException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
            }
        }

        return results;
    }
    @Override
    protected void onPostExecute(HashMap<Value,HashMap<String, Value>> results) {
        super.onPostExecute(results);
        this.mapFragment.getResults(this.flag,results); //return results to map fragment
    }
}
