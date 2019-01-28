/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact;

import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ProposalPackage;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ContextFactory {
    static ContextFactory cf;

    ThreadLocal<Context> context = new ThreadLocal<>();

    static public ContextFactory getInstance() {
        if (cf == null) {
            cf = new ContextFactory();
        }
        return cf;
    }

    public Context createContext(final ChaincodeStub stub) {
        context.set(new NaiveContextRW(stub));
        return context.get();
    }

    public Context getContext() {
        return context.get();
    }

    static class NaiveContextRW implements Context{

        private ChaincodeStub context;

        public NaiveContextRW(ChaincodeStub stub) {
            context = stub;
        }

        @Override
        public List<byte[]> getArgs() {
            return context.getArgs();
        }

        @Override
        public List<String> getStringArgs() {
            return context.getStringArgs();
        }

        @Override
        public String getFunction() {
            return context.getFunction();
        }

        @Override
        public List<String> getParameters() {
            return context.getParameters();
        }

        @Override
        public String getTxId() {
            return context.getTxId();
        }

        @Override
        public String getChannelId() {
            return context.getChannelId();
        }

        @Override
        public Chaincode.Response invokeChaincode(String chaincodeName, List<byte[]> args, String channel) {
            return context.invokeChaincode(chaincodeName, args, channel);
        }

        @Override
        public byte[] getState(String key) {
            return context.getState(key);
        }

        @Override
        public byte[] getStateValidationParameter(String key) {
            return context.getStateValidationParameter(key);
        }

        @Override
        public QueryResultsIterator<KeyValue> getStateByRange(String startKey, String endKey) {
            return context.getStateByRange(startKey, endKey);
        }

        @Override
        public QueryResultsIteratorWithMetadata<KeyValue> getStateByRangeWithPagination(String startKey, String endKey, int pageSize, String bookmark) {
            return context.getStateByRangeWithPagination(startKey, endKey, pageSize, bookmark);
        }

        @Override
        public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String compositeKey) {
            return context.getStateByPartialCompositeKey(compositeKey);
        }

        @Override
        public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String objectType, String... attributes) {
            return context.getStateByPartialCompositeKey(objectType, attributes);
        }

        @Override
        public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(CompositeKey compositeKey) {
            return context.getStateByPartialCompositeKey(compositeKey);
        }

        @Override
        public QueryResultsIteratorWithMetadata<KeyValue> getStateByPartialCompositeKeyWithPagination(CompositeKey compositeKey, int pageSize, String bookmark) {
            return context.getStateByPartialCompositeKeyWithPagination(compositeKey, pageSize, bookmark);
        }

        @Override
        public CompositeKey createCompositeKey(String objectType, String... attributes) {
            return context.createCompositeKey(objectType, attributes);
        }

        @Override
        public CompositeKey splitCompositeKey(String compositeKey) {
            return context.splitCompositeKey(compositeKey);
        }

        @Override
        public QueryResultsIterator<KeyValue> getQueryResult(String query) {
            return context.getQueryResult(query);
        }

        @Override
        public QueryResultsIteratorWithMetadata<KeyValue> getQueryResultWithPagination(String query, int pageSize, String bookmark) {
            return context.getQueryResultWithPagination(query, pageSize, bookmark);
        }

        @Override
        public QueryResultsIterator<KeyModification> getHistoryForKey(String key) {
            return context.getHistoryForKey(key);
        }

        @Override
        public byte[] getPrivateData(String collection, String key) {
            return context.getPrivateData(collection, key);
        }

        @Override
        public byte[] getPrivateDataValidationParameter(String collection, String key) {
            return context.getPrivateDataValidationParameter(collection, key);
        }

        @Override
        public QueryResultsIterator<KeyValue> getPrivateDataByRange(String collection, String startKey, String endKey) {
            return context.getPrivateDataByRange(collection, startKey, endKey);
        }

        @Override
        public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, String compositeKey) {
            return context.getPrivateDataByPartialCompositeKey(collection, compositeKey);
        }

        @Override
        public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, CompositeKey compositeKey) {
            return context.getPrivateDataByPartialCompositeKey(collection, compositeKey);
        }

        @Override
        public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, String objectType, String... attributes) {
            return context.getPrivateDataByPartialCompositeKey(collection, objectType, attributes);
        }

        @Override
        public QueryResultsIterator<KeyValue> getPrivateDataQueryResult(String collection, String query) {
            return context.getPrivateDataQueryResult(collection, query);
        }

        @Override
        public void setEvent(String name, byte[] payload) {
            context.setEvent(name, payload);
        }

        @Override
        public ChaincodeEventPackage.ChaincodeEvent getEvent() {
            return context.getEvent();
        }

        @Override
        public ProposalPackage.SignedProposal getSignedProposal() {
            return context.getSignedProposal();
        }

        @Override
        public Instant getTxTimestamp() {
            return context.getTxTimestamp();
        }

        @Override
        public byte[] getCreator() {
            return context.getCreator();
        }

        @Override
        public Map<String, byte[]> getTransient() {
            return context.getTransient();
        }

        @Override
        public byte[] getBinding() {
            return context.getBinding();
        }

        @Override
        public void putState(String key, byte[] value) {
            context.putState(key, value);
        }

        @Override
        public void setStateValidationParameter(String key, byte[] value) {
            context.setStateValidationParameter(key, value);
        }

        @Override
        public void delState(String key) {
            context.delState(key);
        }

        @Override
        public void putPrivateData(String collection, String key, byte[] value) {
            context.putPrivateData(collection, key, value);
        }

        @Override
        public void setPrivateDataValidationParameter(String collection, String key, byte[] value) {
            context.setPrivateDataValidationParameter(collection, key, value);
        }

        @Override
        public void delPrivateData(String collection, String key) {
            context.delPrivateData(collection, key);
        }

        @Override
        public void putPrivateData(String collection, String key, String value) {
            context.putPrivateData(collection, key, value);
        }

        @Override
        public void putStringState(String key, String value) {
            context.putStringState(key, value);
        }
    }
}
