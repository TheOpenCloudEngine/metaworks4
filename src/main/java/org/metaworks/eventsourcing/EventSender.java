package org.metaworks.eventsourcing;

import org.metaworks.dwr.MetaworksRemoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Created by uengine on 2018. 1. 8..
 */
public class EventSender {

//    @Autowired
//    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendBusinessEvent(String payload) {
        sendBusinessEvent(payload, new ListenableFutureCallback<SendResult<Integer, String>>() {

            @Override
            public void onSuccess(SendResult<Integer, String> result) {
                //handleSuccess(data);
            }

            @Override
            public void onFailure(Throwable ex) {
                ex.printStackTrace();
            }

        });
    }

    public void sendBusinessEventSync(String payload) {
        KafkaTemplate kafkaTemplate = MetaworksRemoteService.getComponent(KafkaTemplate.class);
        kafkaTemplate.send("bpm.topic", payload);
    }

    public void sendBusinessEvent(String payload, ListenableFutureCallback futureCallback) {
        KafkaTemplate kafkaTemplate = MetaworksRemoteService.getComponent(KafkaTemplate.class);

        ListenableFuture<SendResult<Integer, String>> future = kafkaTemplate.send("bpm.topic", payload);
        future.addCallback(futureCallback);
    }

}

