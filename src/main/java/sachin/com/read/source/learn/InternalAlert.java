package sachin.com.read.source.learn;

import lombok.Data;

import java.util.Date;

@Data
public class InternalAlert {
    private String id;

    private String name;

   // private AlertTriggerType trigger;

    private String expression;

    private String providerId;

    private String componentId;

    private String sensorId;

    private Date updatedAt;
}
