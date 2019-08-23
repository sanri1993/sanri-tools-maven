define(['util','dialog','icheck','jsonview'],function (util,dialog) {
    var kafkaAdmin = {};
    var apis = {
        topics:'/kafka/topics',
        logSizes:'/kafka/logSizes',
        create:'/kafka/createTopic',
        drop:'/kafka/deleteTopic'

    }
    kafkaAdmin.init = function () {
        bindEvents();
        $('#data input[type=checkbox]').iCheck({
            checkboxClass: 'icheckbox_square-green'
        });
        kafkaAdmin.conn = util.parseUrl().params.conn;
        $('#adminconn>a>span').text(kafkaAdmin.conn).attr('conn',kafkaAdmin.conn);

        loadTopics();
        return this;
    }

    function loadTopics() {
        var index = layer.load(1, {
          shade: [0.1,'#fff']
        });
        util.requestData(apis.topics,{clusterName:kafkaAdmin.conn},function (topics) {
            layer.close(index);

            var $topics = $('#topics').empty();

            for(var topic in topics){
                var $topic = $('<div class="list-group-item" topic="'+topic+'">'+topic+' <a href="javascript:void(0);" class=" pull-right">删除</a></div>').appendTo($topics);
                $topic.data('partitions',topics[topic]);
            }
        });
    }

    function bindEvents() {
        var events = [{selector:'#createTopic',types:['click'],handler:createTopic},
            {parent:'#topics',selector:'.list-group-item',types:['click'],handler:showTopicDetail},
            {parent:'#topics',selector:'.list-group-item>a',types:['click'],handler:deleteTopic},
            {selector:'#refreshlogsize',types:['click'],handler:refreshLogSize},
            {selector:'#createdata',types:['click'],handler:createData} ];
        util.regPageEvents(events);

        function refreshLogSize() {
            var topic = $('#topics').find('div.list-group-item.active').attr('topic');
            renderTopicPartitions(topic);
        }

        function createTopic() {
            dialog.create('创建主题')
                .setContent($('#createTopicDialog'))
                .setWidthHeight('40%','45%')
                .addBtn({type:'yes',text:'确定',handler:function(index, layero){
                        var params = util.serialize2Json($('#createTopicDialog').find('form').serialize());
                        if(!params.topic || !params.partitions || !params.replication){
                            layer.msg('请将信息填写完整');
                            return ;
                        }
                        params.name = kafkaAdmin.conn;
                        util.requestData(apis.create,params,function () {
                            loadTopics(params.topic);
                            layer.close(index);
                        })
                    }}).build();
        }
        
        function showTopicDetail() {
            $(this).addClass('active').find('a').addClass('text-whitesmoke');
            $(this).siblings().removeClass('active').find('a').removeClass('text-whitesmoke');

            var topicName = $(this).attr('topic');
            $('#topicname').text(topicName);
            $('#topicname').data('topic',topicName);
            renderTopicPartitions(topicName);
        }
        function deleteTopic() {
            var topicName = $(this).parent().attr('topic');
            layer.confirm('确定删除主题:'+topicName,function (r) {
                if(r){
                    util.requestData(apis.drop,{topic:topicName,name:kafkaAdmin.conn},function () {
                        layer.msg('删除成功');
                        loadTopics(); //不管用,因为删除的也加载了
                    });
                }
            });
        }
        
        function createData() {
            
        }
        
        function renderTopicPartitions(topicName) {
            var index = layer.load(1, {
                shade: [0.1,'#fff']
            });
            util.requestData(apis.logSizes, {clusterName:kafkaAdmin.conn,topic: topicName}, function (logSizes) {
                var $tbody = $('#topicdetail>tbody').empty();
                var htmlCode = [];
                var partitions = Object.keys(logSizes);
                $('#topicname').data('partitions', partitions);

                var dateFormat = util.FormatUtil.dateFormat(new Date().getTime(), 'yyyy-MM-dd HH:mm:ss');
                for (var key in logSizes) {
                    htmlCode.push('<tr partition="'+key+'"><td>' + key + '</td><td>' + logSizes[key] + '</td><td>' + (dateFormat) + '</td></tr>')
                }
                $tbody.append(htmlCode.join(''));

                layer.close(index);
            });
        }
    }
    return kafkaAdmin.init();
});