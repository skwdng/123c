package com.feed.server.config;

import com.feed.common.constant.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：
 * 1 个 Fanout 交换机（发布事件广播）+ 5 个持久化队列 + 1 个死信交换机/死信队列
 * 消息体统一使用 Jackson JSON 序列化
 */
@Configuration
public class RabbitMQConfig {

    /**
     * JSON 消息转换器，生产者/消费者自动用它序列化 EventMessage
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 发布事件 Fanout 交换机：同一条消息广播到全部绑定队列，
     * 各 Worker 按 eventType 自行过滤
     */
    @Bean
    public FanoutExchange feedExchange() {
        return new FanoutExchange(MqConstants.FEED_EXCHANGE, true, false);
    }

    /**
     * 死信交换机（Fanout）
     */
    @Bean
    public FanoutExchange feedDlxExchange() {
        return new FanoutExchange(MqConstants.FEED_DLX_EXCHANGE, true, false);
    }

    /**
     * 死信队列：消费失败且不重新入队的消息落到这里，避免无限重试堵塞正常队列
     */
    @Bean
    public Queue feedDlxQueue() {
        return QueueBuilder.durable(MqConstants.DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(feedDlxQueue()).to(feedDlxExchange());
    }

    /**
     * 五个业务队列，全部持久化并挂死信交换机
     */
    @Bean
    public Queue timelineQueue() {
        return businessQueue(MqConstants.TIMELINE_QUEUE);
    }

    @Bean
    public Queue likeQueue() {
        return businessQueue(MqConstants.LIKE_QUEUE);
    }

    @Bean
    public Queue commentQueue() {
        return businessQueue(MqConstants.COMMENT_QUEUE);
    }

    @Bean
    public Queue socialQueue() {
        return businessQueue(MqConstants.SOCIAL_QUEUE);
    }

    @Bean
    public Queue notificationQueue() {
        return businessQueue(MqConstants.NOTIFICATION_QUEUE);
    }

    private Queue businessQueue(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", MqConstants.FEED_DLX_EXCHANGE)
                .build();
    }

    /**
     * 五个队列全部绑定到 Fanout 交换机（每个绑定单独声明为 Bean，RabbitAdmin 才能自动创建）
     */
    @Bean
    public Binding timelineBinding() {
        return BindingBuilder.bind(timelineQueue()).to(feedExchange());
    }

    @Bean
    public Binding likeBinding() {
        return BindingBuilder.bind(likeQueue()).to(feedExchange());
    }

    @Bean
    public Binding commentBinding() {
        return BindingBuilder.bind(commentQueue()).to(feedExchange());
    }

    @Bean
    public Binding socialBinding() {
        return BindingBuilder.bind(socialQueue()).to(feedExchange());
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(feedExchange());
    }
}
