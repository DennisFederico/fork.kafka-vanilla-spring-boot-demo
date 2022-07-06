package com.example.demo.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogAndFailExceptionHandler implements KafkaExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(LogAndFailExceptionHandler.class);

    private final OnFatalErrorListener defaultOnFatalErrorListener;

    public LogAndFailExceptionHandler() {
        this(new PropagateFatalErrorListener());
    }

    public LogAndFailExceptionHandler(OnFatalErrorListener onFatalErrorListener) {
        this.defaultOnFatalErrorListener = onFatalErrorListener;
    }

    @Override
    public <K, V> void handleProcessingError(
            ConsumerRecord<DeserializerResult<K>, DeserializerResult<V>> record,
            Exception exception,
            OnSkippedRecordListener onSkippedRecordListener,
            OnFatalErrorListener onFatalErrorListener) {
        logger.error("Exception caught during processing, topic: " + record.topic() + ", partition: " + record.partition() + ", offset: " + record.offset(), exception);
        fireOnFatalErrorEvent(ErrorType.PROCESSING_ERROR, record, exception, onFatalErrorListener);
    }

    @Override
    public <K, V> void handleDeserializationError(
            ConsumerRecord<DeserializerResult<K>, DeserializerResult<V>> record,
            OnValidRecordListener onValidRecordListener,
            OnSkippedRecordListener onSkippedRecordListener,
            OnFatalErrorListener onFatalErrorListener) {
        if (record.key() != null && !record.key().valid()) {
            logger.error("Exception caught during Deserialization of the key, topic: " + record.topic() + ", partition: " + record.partition() + ", offset: " + record.offset(), record.key().getException());
            fireOnFatalErrorEvent(ErrorType.DESERIALIZATION_ERROR, record, record.key().getException(), onFatalErrorListener);
            return;
        }

        if (record.key() != null && !record.value().valid()) {
            logger.error("Exception caught during Deserialization of the key, topic: " + record.topic() + ", partition: " + record.partition() + ", offset: " + record.offset(), record.key().getException());
            fireOnFatalErrorEvent(ErrorType.DESERIALIZATION_ERROR, record, record.key().getException(), onFatalErrorListener);
            return;
        }
        onValidRecordListener.onValidRecordEvent();
    }

    private <K, V> void fireOnFatalErrorEvent(ErrorType errorType, ConsumerRecord<DeserializerResult<K>, DeserializerResult<V>> record, Exception exception, OnFatalErrorListener onFatalErrorListener) {
        if (onFatalErrorListener != null) {
            onFatalErrorListener.onFatalErrorEvent(errorType, record, exception);
        } else {
            defaultOnFatalErrorListener.onFatalErrorEvent(errorType, record, exception);
        }
    }
}
