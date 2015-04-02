package cmuibm;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;


import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Path("/parser")   //http://localhost:8080/webapi/parser
@Produces("text/plain")
public class TradeOffParser {
    @GET
    @Path("/allkey")
    @Produces("text/plain")
    public String get_allKey(@QueryParam("companies") String complist)
            throws JSONException {
        String[] complist_arr = complist.split(",");//get company list
        JSONArray comp_data = new JSONArray();
        for(int i = 0; i < complist_arr.length; i++){
            JSONObject jo = crawl_generate(complist_arr[i]);
            comp_data.put(jo);
        }
        HashSet<String> hs = get_key(comp_data);
        JSONArray keywords = new JSONArray();
        Iterator iter = hs.iterator();
        while(iter.hasNext()){
            keywords.put(iter.next());
        }
        return "callback("+keywords.toString()+")\n";
    }

    @GET
    @Path("/select")
    @Produces("text/plain")
    public String get_seleKey(@QueryParam("companies") String complist,
                              @QueryParam("values") String valuelist)
            throws JSONException {
        String[] complist_arr = complist.split(",");
        String[] valuelist_arr = valuelist.split(",");

        JSONArray comp_data = new JSONArray();

        String subject = "Risk";
        for(int i = 0; i < complist_arr.length; i++){
            JSONObject jo = crawl_generate(complist_arr[i]);
            comp_data.put(jo);
        }
        JSONObject resd = new JSONObject();
        JSONArray columns = new JSONArray();
        HashSet nk = new HashSet();
        for(int i = 0; i < valuelist_arr.length; i++){
            JSONObject tmpjo = new JSONObject();
            tmpjo.put("key", valuelist_arr[i]);
            tmpjo.put("full_name", valuelist_arr[i]);
            tmpjo.put("type", "numeric");
            tmpjo.put("is_objective", "TRUE");
            tmpjo.put("goal", "MIN");
            JSONArray ele = new JSONArray();
            ele.put(tmpjo);
            columns.put(ele);
            nk.add(valuelist_arr[i]);
        }
        resd.put("columns", columns);
        resd.put("subject", subject);
        JSONArray pd = new JSONArray();
        pd = parser(comp_data, nk);
        resd.put("options", pd);
        return "callback("+resd.toString()+")\n";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get_comp(@QueryParam("company") String complist) throws JSONException {
        String[] complist_arr = complist.split(",");//get company list
        JSONArray comp_data = new JSONArray();

        String subject = "Risk";
        for(int i = 0; i < complist_arr.length; i++){
            JSONObject jo = crawl_generate(complist_arr[i]);
            comp_data.put(jo);
        }
        JSONObject resd = new JSONObject();
        HashSet<String> hs = get_key(comp_data);
        String[] keywords = hs.toArray(new String[hs.size()]);
        JSONArray columns = new JSONArray();
        HashSet nk = new HashSet();
        for(int i = 0; i < 10; i++){
            JSONObject tmpjo = new JSONObject();
            tmpjo.put("key", keywords[i]);
            tmpjo.put("full_name", keywords[i]);
            tmpjo.put("type", "numeric");
            tmpjo.put("is_objective", "TRUE");
            tmpjo.put("goal", "MIN");
            JSONArray ele = new JSONArray();
            ele.put(tmpjo);
            columns.put(ele);
            nk.add(keywords[i]);
        }
        hs = new HashSet(nk);
        resd.put("columns", columns);
        resd.put("subject", subject);
        JSONArray pd;
        pd = parser(comp_data, nk);
        resd.put("options", pd);
        return resd.toString()+'\n';
    }

    public JSONObject crawl_generate(String company_name) throws JSONException{
        JSONObject json = new JSONObject();
        try {
            json = new JSONObject(IOUtils.toString(new URL("http://riskanalysis.mybluemix.net/api/results/"+company_name), Charset.forName("UTF-8")));
        } catch (Exception ex) {
            System.err.println(ex);
        }
        JSONArray ja = (JSONArray) json.get("records");
        if(ja.length()!=0){
            json.put("companyName", company_name);
            return json;
        }
        else{
            try {
                URL tmp = new URL("riskanalysis.mybluemix.net/api/crawl/"+company_name);
                json = new JSONObject(IOUtils.toString(new URL("http://riskanalysis.mybluemix.net/api/results/"+company_name), Charset.forName("UTF-8")));
            } catch (Exception ex) {
                System.err.println(ex);
            }
            json.put("companyName", company_name);
            return json;
        }
    }

    public HashSet<String> get_key(JSONArray data) throws JSONException{
        HashSet<String> rset = new HashSet<String>();
        HashSet<String> hs = new HashSet<String>();
        JSONObject jo = data.getJSONObject(0);
        JSONArray joo = (JSONArray) jo.get("records");
        JSONObject res = (JSONObject) joo.getJSONObject(0).get("keywords");
        //add jsonobject into set
        Iterator keys = res.keys();
        while(keys.hasNext()){
            String key = (String)keys.next();
            rset.add(key);
        }
        //rset.add(res);
        for(int i = 1; i < data.length(); i++){
            jo = data.getJSONObject(1);
            joo = (JSONArray) jo.get("records");
            res = (JSONObject)joo.getJSONObject(0).get("keywords");
            //add jsonobject into set
            keys = res.keys();
            while(keys.hasNext()){
                String key = (String)keys.next();
                //intersection
                if(rset.contains(key)){
                    hs.add(key);
                }
            }
        }
        return hs;
    }

    public HashMap<String, Integer> filter_keyswords(HashMap<String, Integer> keywords, HashSet<String> filter_set){
        HashMap<String, Integer> filteredKeys = new HashMap<String, Integer>();
        for(Entry<String, Integer> entry : keywords.entrySet()){
            if(filter_set.contains(entry.getKey())){
                filteredKeys.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredKeys;
    }

    public JSONArray parser(JSONArray d, HashSet<String> nk) throws JSONException{
        JSONArray options = new JSONArray();
        int index = 0;
        for(int i = 0; i < d.length(); i++){
            JSONObject data = d.getJSONObject(i);
            for( int j = 0; j < data.length(); j++){
                JSONArray entry = (JSONArray) data.get("records");
                JSONObject valuesJSON = (JSONObject)entry.getJSONObject(j).get("keywords");
                HashMap<String, Integer> values = new HashMap<String, Integer>();
                Iterator keys = valuesJSON.keys();
                while(keys.hasNext()){
                    String key = (String) keys.next();
                    values.put(key, Integer.parseInt(valuesJSON.get(key).toString()));
                }
                values = filter_keyswords(values, nk);

                JSONObject ele = new JSONObject();
                ele.put("key", String.valueOf(index));
                StringBuilder tmp = new StringBuilder();
                tmp.append(data.get("companyName"));
                tmp.append(entry.getJSONObject(j).get("year"));
                ele.put("name",tmp.toString());
                ele.put("values", values);
                ele.put("description_html", "Select Biotechnology Portfolio");

                options.put(ele);
                index += 1;
            }
        }
        return options;
    }
}


