var template;
{
    var xhr = new XMLHttpRequest();
    xhr.open('GET', "http://rawgit.com/TheOpenCloudEngine/metaworks4/master/src/main/resources/static/iam-avatar.html", false);
    xhr.onload = function () {
        template = xhr.responseText
    }
    xhr.send();
}


Vue.component('iam-avatar', {
    template: template,
    props: {
    },
    data: function(){

        return {

            photoUrl: localStorage['iam.photo_url']

        }

    },

    methods: {
       logout: function (){
       }
    }

})

