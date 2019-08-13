define(['util','dialog'],function (util,dialog) {
    var redisclient = {};
    var apis = {
        connNames:'/file/manager/simpleConfigNames',
        createConn:'/file/manager/writeConfig',
        detail:'/file/manager/readConfig',
        serializes:'/zk/serializes'
    };
    var modul = 'redis';

    function initApis(){
        var methods = ['redisInfo','dbs','dbSize','scan','keys','lrange','zscan','hscan','type','readValue','readHashValue','keyLength','ttl','pttl','batchDelete'];
        methods.forEach(function (method) {
            apis[method] = '/redis/'+method;
        });
    }

    redisclient.init = function () {
        initApis();
        loadSerializes();
        bindEvent();
        
        loadConns(function () {
            $('#connect>.dropdown-menu>li:first').click();
            $('#connect>.dropdown-menu').dropdown('toggle');
        });
        
        return this;
    }

    /**
     * 加载所有序列化工具
     */
    function loadSerializes() {
        util.requestData(apis.serializes,function (serializes) {
            var $serializes =  $('#nodedata').find('select[name=deserialize]').empty();
            for(var i=0;i<serializes.length;i++){
                $serializes.append('<option value="'+serializes[i]+'">'+serializes[i]+'</option>')
            }
            //选中第一个
            $serializes.val(serializes[0]);
        });
    }

    /**
     * 加载所有的连接
     * @param callback
     */
    function loadConns(callback) {
        util.requestData(apis.connNames,{modul:modul},function (conns) {
            var $menu = $('#connect>ul.dropdown-menu').empty();
            if(conns){
                for(var i=0;i<conns.length;i++){
                    var $item = $('<li name="'+conns[i]+'"><a href="javascript:void(0);">'+conns[i]+'</a></li>').appendTo($menu);
                    $item.data('value',conns[i]);
                }
                if(callback){
                    callback(conns);
                }
            }
        });
    }

    /**
     * 加载所有数据库
     */
    function loadDatabases() {
        util.requestData(apis.dbs,{name:redisclient.conn},function (dbs) {
           console.log(dbs);
        });
    }
    
    function bindEvent() {
        var events = [{parent:'#connect>.dropdown-menu',selector:'li',types:['click'],handler:switchConn},
            {selector:'#newconnbtn',types:['click'],handler:newconn}];
        
        util.regPageEvents(events);

        /**
         * 切换连接 
         */
        function switchConn() {
            var conn = $(this).data('value');
            redisclient.conn = conn;

            $('#connect>button>span:eq(0)').text(conn);
            util.requestData(apis.detail,{modul:modul,baseName:conn},function (address) {
                $('#connect').next('input').val(address);
            });
            $('#connect>.dropdown-menu').dropdown('toggle');

            loadConns(function () {
                loadDatabases();
            });
        }

        /**
         * 新连接
         */
        function newconn() {
            dialog.create('新连接')
                .setContent($('#newconn'))
                .setWidthHeight('60%','300px')
                .addBtn({type:'yes',text:'确定',handler:function(index, layero){
                        var params = util.serialize2Json($('#newconn>form').serialize());
                        if(!params.name || !params.connectStrings){
                            layer.msg('请将信息填写完整');
                            return ;
                        }
                        params.modul = modul;
                        params.baseName = params.name;
                        params.content = JSON.stringify({connectStrings:params.connectStrings,auth:params.auth});
                        util.requestData(apis.createConn,params,function () {
                            layer.close(index);

                            loadConns(function (conns) {
                                if(conns){
                                    //请求最后一个连接,并选中
                                    $('#connect>.dropdown-menu>li[name='+params.name+']').click();
                                    $('#connect>.dropdown-menu').dropdown('toggle');
                                }
                            });
                        });
                    }})
                .build();
        }
    }

    return redisclient.init();
});