package net.liquidcars.ingestion.application.service.parser.model.XML;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper= false)
public class KeyValueXMLModel<K extends Serializable,V extends Serializable> extends DefaultKeyValue<K,V> implements Serializable {
    private static final String[] DEFAULT_KEYS = new String[] {"?", "UNKNOWN","OTHER","NONE"};
    public KeyValueXMLModel(){
        super();
    }
    public KeyValueXMLModel(final K key, final V value) {
        super(key, value);
    }

    public static KeyValueXMLModel getDefault(List<KeyValueXMLModel> KeyValueXMLModelList){
        if (KeyValueXMLModelList !=null && !KeyValueXMLModelList.isEmpty()) {
            return KeyValueXMLModelList.stream().filter(x-> {
                for (String key : DEFAULT_KEYS) {
                    if (x.getKey().toString().toUpperCase().equals(key)) return true;
                }
                return false;
            }).findFirst().orElse(null);
        }
        return null;
    }

    @Override
    public String toString() {
        String k = (this.getKey() != null && !this.getKey().toString().isEmpty())
                ? this.getKey().toString() : "";
        String v = (this.getValue() != null && !this.getValue().toString().isEmpty())
                ? this.getValue().toString() : "";
        return "[" + k + " / " + v + "]";
    }

    public static Map<String, String> toMap(List<KeyValueXMLModel> items) {
        if (items == null) return null;
        if (items.isEmpty()) return new HashMap<String,String>();
        return items.stream()
                .collect(Collectors.toMap(
                        kv -> (String) kv.getKey(),
                        kv -> (String) kv.getValue(),
                        (v1, v2) -> v1  // In case of duplicate keys, keep the first value
                ));
    }
}
