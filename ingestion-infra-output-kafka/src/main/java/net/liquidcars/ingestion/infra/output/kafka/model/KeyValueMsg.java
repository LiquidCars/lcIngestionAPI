package net.liquidcars.ingestion.infra.output.kafka.model;

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
public class KeyValueMsg<K extends Serializable,V extends Serializable> extends DefaultKeyValue<K,V> implements Serializable {

    private static final String[] DEFAULT_KEYS = new String[] {"?", "UNKNOWN","OTHER","NONE"};
    public KeyValueMsg(){
        super();
    }
    public KeyValueMsg(final K key, final V value) {
        super(key, value);
    }

    public static KeyValueMsg getDefault(List<KeyValueMsg> keyValueMsgList){
        if (keyValueMsgList !=null && !keyValueMsgList.isEmpty()) {
            return keyValueMsgList.stream().filter(x-> {
                for (String key : DEFAULT_KEYS) {
                    if (x.getKey().toString().toUpperCase().equals(key)) return true;
                }
                return false;
            }).findFirst().orElse(null);
        }
        return null;
    }

    @Override
    public String toString(){
        return "[" + this.getKey()!=null && !this.getKey().toString().isEmpty() ? this.getKey().toString() : "" + " / " +
                this.getValue()!=null && !this.getValue().toString().isEmpty() ? this.getValue().toString() : "" + "]";
    }

    public static Map<String, String> toMap(List<KeyValueMsg> items) {
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
