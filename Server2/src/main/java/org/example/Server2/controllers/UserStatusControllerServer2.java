package org.example.Server2.controllers;

import org.example.Common.entities.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/client")
public class UserStatusControllerServer2 {


    @PostMapping("/statusChecked")
    public ResponseEntity <Map<String, Object>> checkClientStatus(@RequestBody Map<String, Object> request)  {
        Long clientId = Long.valueOf(request.get("clientId").toString());
        Long accountId = Long.valueOf(request.get("accountId").toString());

        boolean checkStatus = Math.random() < 0.7;
        User.StatusUser status = checkStatus ? User.StatusUser.ACTIVE : User.StatusUser.BLOCKED;

        Map<String, Object> response = Map.of(
                "clientId", clientId,
                "accountId", accountId,
                "status", status.name()
        );
        return ResponseEntity.ok(response);
    }

}
