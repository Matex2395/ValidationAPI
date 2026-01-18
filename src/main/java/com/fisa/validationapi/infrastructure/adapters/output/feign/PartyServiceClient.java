package com.fisa.validationapi.infrastructure.adapters.output.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign para comunicarse con el microservicio Party Service Operation.
 * Actúa como un proxy HTTP declarativo.
 */
// "name" es el identificador del servicio (útil si usaras Eureka, pero requerido siempre)
// "url" se inyecta desde application.yml (ej: http://localhost:8090)
@FeignClient(name = "party-service", url = "${party.service.url}")
public interface PartyServiceClient {

    /**
     * Envía la solicitud de creación de cliente al Party Service.
     * * La ruta debe coincidir EXACTAMENTE con el PartyController:
     * @RequestMapping("/bian-party/v1") + @PostMapping("/parties")
     * * @param partyJson El cuerpo del mensaje validado (JSON String).
     * @return La respuesta del servicio (uso de Object para ser genérico y no acoplarnse al modelo Party).
     */
    @PostMapping("/bian-party/v1/parties")
    ResponseEntity<Object> createParty(@RequestBody String partyJson);
}