//package io.project.clientkeeperbot.state;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.statemachine.StateMachine;
//import org.springframework.statemachine.config.StateMachineFactory;
//import org.springframework.statemachine.persist.StateMachinePersister;
//import org.springframework.statemachine.service.StateMachineService;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class BotStateManager {
//
//
//    private final StateMachineService<BotState, BotEvent> stateMachineService;
//
//    public void processEvent(String userId, BotEvent event) {
//        StateMachine<BotState, BotEvent> stateMachine = stateMachineService.acquireStateMachine(userId);
//        stateMachine.sendEvent(event);
//    }
//
//    public BotState getCurrentState(String userId) {
//        StateMachine<BotState, BotEvent> stateMachine = stateMachineService.acquireStateMachine(userId);
//        return stateMachine.getState().getId();
//    }
//}