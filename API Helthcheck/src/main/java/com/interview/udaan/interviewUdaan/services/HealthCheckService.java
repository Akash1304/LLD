package com.interview.udaan.interviewUdaan.services;

import com.interview.udaan.interviewUdaan.entities.Instances;
import com.interview.udaan.interviewUdaan.entities.Pings;
import com.interview.udaan.interviewUdaan.entities.Services;
import com.interview.udaan.interviewUdaan.enums.HealthStatus;
import com.interview.udaan.interviewUdaan.repos.InstancesRepo;
import com.interview.udaan.interviewUdaan.repos.PingsRepo;
import com.interview.udaan.interviewUdaan.repos.ServicesRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HealthCheckService {

    @Autowired
    ServicesRepo servicesRepo;

    @Autowired
    InstancesRepo instancesRepo;

    @Autowired
    PingsRepo pingsRepo;

    public void checkAndUpdateHealth(){
        List<Services> services =servicesRepo.findAll();
        for(Services service: services){
            List<Instances> instances = instancesRepo.findAllByServiceIdFk(service.getId());
            int instanceCount = instances.size();
            int successInstances = 0;
            for(Instances instance: instances){
                List<Pings> last3MinPings = pingsRepo.findPingsInLast3Min(service.getId(),instance.getId());
                List<Pings> last2MinPings = pingsRepo.findPingsInLast2Min(service.getId(),instance.getId());
                List<Pings> last1MinPings = pingsRepo.findPingsInLast1Min(service.getId(),instance.getId());
                int last3MinSize = last3MinPings.size();
                if(last3MinSize<9) continue;
                int last2MinSize = last2MinPings.size();
                int last1MinSize = last1MinPings.size();

                int size2min = last2MinSize - last1MinSize;
                if(size2min <3) continue;
                int size3min = last2MinSize - last2MinSize - last1MinSize;
                if(size3min< 3) continue;
                successInstances++;
            }
            setHealthStatus(service,instanceCount,successInstances);
            System.out.println("Service: " + service.getName() + " have status: " + service.getHealthStatus() + " Success Instances:" + successInstances);
        }
    }

    private void setHealthStatus(Services service, int instanceCount, int successInstances) {
        if(instanceCount/2 >= successInstances){
            service.setHealthStatus(HealthStatus.RED);
        }
        else if(instanceCount/2 <= successInstances){
            service.setHealthStatus(HealthStatus.ORANGE);
        }else if(instanceCount == successInstances){
            service.setHealthStatus(HealthStatus.GREEN);
        }
    }
}
