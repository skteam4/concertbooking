package concertbooking;

import concertbooking.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired AlarmRepository alarmRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBooked_EventReceive(@Payload Booked booked){

        if(!booked.validate()) return;

        System.out.println("\n\n##### listener EventReceive : " + booked.toJson() + "\n\n");

        // Sample Logic //
        Alarm alarm = new Alarm();
        alarmRepository.save(alarm);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryStarted_EventReceive(@Payload DeliveryStarted deliveryStarted){

        if(!deliveryStarted.validate()) return;

        System.out.println("\n\n##### listener EventReceive : " + deliveryStarted.toJson() + "\n\n");

        // Sample Logic //
        Alarm alarm = new Alarm();
        alarmRepository.save(alarm);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCanceled_EventReceive(@Payload DeliveryCanceled deliveryCanceled){

        if(!deliveryCanceled.validate()) return;

        System.out.println("\n\n##### listener EventReceive : " + deliveryCanceled.toJson() + "\n\n");

        // Sample Logic //
        Alarm alarm = new Alarm();
        alarmRepository.save(alarm);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
