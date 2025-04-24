//package io.project.clientkeeperbot.state;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.statemachine.config.EnableStateMachineFactory;
//import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
//import org.springframework.statemachine.config.StateMachineFactory;
//import org.springframework.statemachine.config.builders.*;
//import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
//import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
//import org.springframework.statemachine.persist.StateMachineRuntimePersister;
//import org.springframework.statemachine.service.DefaultStateMachineService;
//import org.springframework.statemachine.service.StateMachineService;
//
//@Configuration
//@EnableStateMachineFactory
//@EnableJpaRepositories(
//        basePackages = "org.springframework.statemachine.data.jpa",
//        entityManagerFactoryRef = "stateMachineEmf",
//        transactionManagerRef = "stateMachineTransactionManager"
//)
//public class StateMachineConfig extends StateMachineConfigurerAdapter<BotState, BotEvent> {
//
//    @Override
//    public void configure(StateMachineStateConfigurer<BotState, BotEvent> states) throws Exception {
//        states.withStates()
//                .initial(BotState.START)
//                .state(BotState.WAITING_CAPTCHA)
//                .state(BotState.MAIN_MENU)
//                .state(BotState.CREATING_TICKET);
//    }
//
//    @Override
//    public void configure(StateMachineTransitionConfigurer<BotState, BotEvent> transitions) throws Exception {
//        transitions
//                .withExternal()
//                .source(BotState.START).target(BotState.WAITING_CAPTCHA).event(BotEvent.REGISTER)
//                .and()
//                .withExternal()
//                .source(BotState.WAITING_CAPTCHA).target(BotState.MAIN_MENU).event(BotEvent.CAPTCHA_PASSED)
//                .and()
//                .withExternal()
//                .source(BotState.MAIN_MENU).target(BotState.CREATING_TICKET).event(BotEvent.CREATE_TICKET_SELECTED);
//    }
//
//    @Bean
//    public StateMachineRuntimePersister<BotState, BotEvent, String> stateMachineRuntimePersister(
//            JpaStateMachineRepository jpaStateMachineRepository) {
//        return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
//    }
//
//    @Bean
//    public StateMachineService<BotState, BotEvent> stateMachineService(
//            StateMachineFactory<BotState, BotEvent> stateMachineFactory,
//            StateMachineRuntimePersister<BotState, BotEvent, String> stateMachineRuntimePersister) {
//        return new DefaultStateMachineService<>(stateMachineFactory, stateMachineRuntimePersister);
//    }
//}