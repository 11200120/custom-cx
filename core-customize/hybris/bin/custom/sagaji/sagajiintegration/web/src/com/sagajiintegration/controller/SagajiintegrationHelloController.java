/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sagajiintegration.controller;

import com.sagajiintegration.service.SagajiintegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
public class SagajiintegrationHelloController {
    
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String printWelcome(final ModelMap model) {

        return "welcome";
    }
}
