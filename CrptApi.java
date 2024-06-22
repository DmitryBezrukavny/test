package test;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper; 


import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


public class CrptApi {
    private List<Date> time_queue=new LinkedList<Date>();
    private Queue<Message> message_queue=new LinkedList<Message>();
    private TimeUnit timeUnit;
    private int requestLimit;
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit=timeUnit;
        this.requestLimit=requestLimit;
        Loader loader=new Loader();
        Thread th=new Thread(loader);
        th.start();
    }
    
    public void sendData(Object data, String signature){
        try{
            ObjectMapper ow = new ObjectMapper();
            String json = ow.writeValueAsString(data);
            Message mes=new Message();
            mes.message=json;
            mes.signature=signature;
            message_queue.add(mes);
        }catch(Exception ex){
            
        }
    }
    
    private long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }
    
    private class Loader implements Runnable{
        @Override
        public void run(){
            while(true){
                try{
                    while(!message_queue.isEmpty()){
                        boolean flag=false;
                        Message mes=message_queue.poll();
                        while(!flag){
                            flag=isPlaceForNewMessage();
                            if(flag){
                                Sender snd= new Sender(mes);
                                Thread th=new Thread(snd);
                                th.start();
                            }
                            Thread.sleep(10); 
                        }
                        
                    }
                Thread.sleep(10);    
                }catch(Exception ex){}
                
            }
        }
    }
    private boolean isPlaceForNewMessage(){
        if (time_queue.size()<requestLimit){
            time_queue.add(new Date());
            return true;
        }else{
            boolean flag=false;
            while(getDateDiff(time_queue.get(0), new Date(), timeUnit)>=1){
                time_queue.remove(0);
                flag=true;
                if(time_queue.size()==0) break;
            }
            if (flag) time_queue.add(new Date());
            return flag;
        }
    }
    
    private class Sender implements Runnable{
        Message mes;
        public Sender(Message mes){
            this.mes=mes;
        }
        @Override
        public void run(){
            try{    
                String result="";
                String uri = "https://ismp.crpt.ru/api/v3/lk/documents/create";
                HttpPost httpPost = new HttpPost(uri);
                StringEntity entity = new StringEntity(mes.message);
                httpPost.setEntity(entity);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");
                httpPost.setHeader("signature", mes.signature);
                try (CloseableHttpClient httpClient = HttpClients.createDefault();
                    CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    result = EntityUtils.toString(response.getEntity());
                }

               
            }catch(Exception ex){}
            
        }
    }    
    
    
    
    private class Message{
        public String message;
        public String signature;
    }
}
