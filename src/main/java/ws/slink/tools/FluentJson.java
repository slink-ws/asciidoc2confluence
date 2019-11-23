package ws.slink.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 *   implementation of Fluent Interface for org.json.simple
 */
public class FluentJson implements Iterable<Object> {
    private Object value;

    public FluentJson () {
        try {
            this.value = new JSONParser().parse("{}");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    public FluentJson (Object value) {
        this.value = value;
    }
    public FluentJson (String jsonStr) throws ParseException {
        this.value = new JSONParser().parse(jsonStr);
    }

    public static FluentJson copy(FluentJson jsonObj) {
        return new FluentJson(jsonObj.value);
    }

    @Override
    public Iterator<Object> iterator() {
        JSONArray a = (JSONArray) value;
        return a.iterator();
    }
    public Stream<FluentJson> stream() {
        JSONArray a = (JSONArray) value;
        return a.stream().map(FluentJson::new);
    }
    public Stream<String> keys() {
        JSONObject o = (JSONObject) value;
        return o.keySet().stream();
    }

    public Object get() {
        return value;
    }

    public FluentJson get(int index) {
        JSONArray a = (JSONArray) value;
        return new FluentJson(a.get(index));
    }
    public FluentJson get(String key) {
//        System.out.println("VALUE CLASS: " + value.getClass().getName());
//        JSONObject o = null;
//        if (value instanceof JSONObject)
//            o = (JSONObject) value;
//        else if (value instanceof FluentJson)
//            o = (JSONObject)((FluentJson)value).value;
        JSONObject o = (JSONObject) value;
        if (null == o.get(key))
            return null;
        else
            return new FluentJson(o.get(key));
    }

    public FluentJson add(FluentJson json) {
        if (json.value instanceof JSONObject) {
            json.keys().forEach(k -> this.set(k, json.get(k)));
        } else if (json.value instanceof JSONArray) {
            JSONArray a = (JSONArray)json.value;
            json.stream().forEach(v -> a.add(v));
        }
        return this;
    }

    public FluentJson merge(FluentJson json) {
        json.keys().forEach(newKey -> {
            FluentJson child = this.get(newKey);
            if (null == child || child.isEmpty())
                this.set(newKey, json.get(newKey));
            else {
//                this.set(newKey, new FluentJson());
                child.merge(json.get(newKey));
            }
        });
        return this;
    }

    public int intValue() {
        return Integer.parseInt(value.toString());
    }

    public int getInt(String key) {
        JSONObject o = (JSONObject) value;
        return new FluentJson(o.get(key)).intValue();
    }

    public double doubleValue() {
        return Double.parseDouble(value.toString());
    }

    public double getDouble(String key) {
        JSONObject o = (JSONObject) value;
        return new FluentJson(o.get(key)).doubleValue();
    }
    public String toString() {
//        return value == null ? null : value.toString();
        ObjectMapper om = new ObjectMapper();
        try {
            return om.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
    public String getString(String key) {
        JSONObject o = (JSONObject) value;
        return new FluentJson(o.get(key)).toString();
    }
    public Number toNumber() {
        return (Number) value;
    }

    public boolean boolValue() {
        return Boolean.parseBoolean(value.toString());
    }
    public boolean getBoolean(String key) {
        JSONObject o = (JSONObject) value;
        return new FluentJson(o.get(key)).boolValue();
    }

    public FluentJson set(Object key, Object val) {
        JSONObject o = (JSONObject) value;
        if (val instanceof FluentJson) {
            o.put(key, ((FluentJson) val).value);
        } else {
            o.put(key, val);
        }
        return this;
    }

    public boolean isEmpty() {
        if (null == value)
            throw new IllegalArgumentException("value is null");
        else if (this.value instanceof JSONObject)
            return ((JSONObject) value).isEmpty();
        else if (this.value instanceof JSONArray)
            return ((JSONArray) value).isEmpty();
        else
            throw new IllegalArgumentException("value object is neither JSONObject nor JSONArray: " + value.getClass().getName());
    }

//    public String toPrettyString(int indentFactor) {
//        org.json.JSONObject json = new org.json.JSONObject(this.toString());
//        return json.toString(indentFactor);
//    }

    public static final String JSON_STR = "{\"entries\": [{\"runs\": [{\"id\": 11}, {\"id\": 12}, {\"id\": 13}]},{\"runs\": [{\"id\": 21}, {\"id\": 22}, {\"id\": 23}]}]}";
    public static void main(String [] args) throws ParseException {
        FluentJson fj = new FluentJson(new JSONParser().parse(JSON_STR));
        String run_id = fj.get("entries")
                          .get(0)
                          .get("runs")
                          .get(0)
                          .get("id")
                          .toString();
        System.out.println(run_id);
    }
}
