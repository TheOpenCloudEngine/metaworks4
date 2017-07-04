var template;
{
    var xhr = new XMLHttpRequest();
    xhr.open('GET', "http://raw.git.com/TheOpenCloudEngine/metaworks4/master/src/main/resources/static/class-selector.html", false);
    xhr.onload = function () {
        template = xhr.responseText
    }
    xhr.send();
}


Vue.component('class-selector', {
    template: template,
    props: {
        data: String,
    },

    data:function(){

        return {
            classTypes: [
                {
                    displayName: '문자열',
                    className: 'java.lang.String'
                },
                {
                    displayName: '정수형',
                    className: 'java.lang.Integer'
                },
                {
                    displayName: '정수형(Long)',
                    className: 'java.lang.Long'
                },
                {
                    displayName: '정수형(long)',
                    className: 'long'
                },
                {
                    displayName: '실수형',
                    className: 'java.lang.double'
                },
                {
                    displayName: '예 아니오',
                    className: 'java.lang.Boolean'
                },
                {
                    displayName: '날짜',
                    className: 'java.util.Date'
                },
            ]
        };
    },

})

