package com.doctorapp.skill;


import com.amazon.ask.model.Directive;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.alexa.rtcsessioncontroller.InitiateSessionWithOfferDirective;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class TelehealthSkillStreamHandler {

    @PostMapping(value = "/alexa/telehealth/skill/directive")
    public Response handleRequest(@RequestBody Directive directive) {
        if (directive instanceof InitiateSessionWithOfferDirective) {
            InitiateSessionWithOfferDirective initiateSessionWithOfferDirective = (InitiateSessionWithOfferDirective) directive;
            log.info("/alexa/telehealth/skill/directive received: {}", initiateSessionWithOfferDirective.toString());
        }

        return null;
    }
}
