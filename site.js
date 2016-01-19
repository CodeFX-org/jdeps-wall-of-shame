
$.extend($.easing,
{
    def: 'easeOutQuad',
    easeInOutExpo: function (x, t, b, c, d) {
        if (t==0) return b;
        if (t==d) return b+c;
        if ((t/=d/2) < 1) return c/2 * Math.pow(2, 10 * (t - 1)) + b;
        return c/2 * (-Math.pow(2, -10 * --t) + 2) + b;
    }
});

window.navScrollerDefaultSettings = {
	scrollToOffset: 210,
	scrollSpeed: 800,
	activateParentNode: true,
};

(function( $ ) {

    var settings;
    var disableScrollFn = false;
    var navItems;
    var navs = {}, sections = {};

    $.fn.navScroller = function(options) {
        settings = $.extend(window.navScrollerDefaultSettings, options );
        navItems = this;

        //attatch click listeners
    	navItems.on('click', function(event){
    		event.preventDefault();
            var navID = $(this).attr("href").substring(1);
            disableScrollFn = true;
            activateNav(navID);
            populateDestinations(); //recalculate these!
        	$('html,body').animate({scrollTop: sections[navID] - settings.scrollToOffset},
                settings.scrollSpeed, "easeInOutExpo", function(){
                    disableScrollFn = false;
                }
            );
    	});

        //populate lookup of clicable elements and destination sections
        populateDestinations(); //should also be run on browser resize, btw

        // setup scroll listener
        $(document).scroll(function(){
            if (disableScrollFn) { return; }
            var page_height = $(window).height();
            var pos = $(this).scrollTop();
            for (i in sections) {
                if ((pos + settings.scrollToOffset >= sections[i]) && sections[i] < pos + page_height){
                    activateNav(i);
                }
            }
        });
    };

    function populateDestinations() {
        navItems.each(function(){
            var scrollID = $(this).attr('href').substring(1);
            navs[scrollID] = (settings.activateParentNode)? this.parentNode : this;
            sections[scrollID] = $(document.getElementById(scrollID)).offset().top;
        });
    }

    function activateNav(navID) {
        for (nav in navs) { $(navs[nav]).removeClass('active'); }
        $(navs[navID]).addClass('active');
    }
})( jQuery );


$(document).ready(function (){

    $('nav li a').navScroller();

    //section divider icon click gently scrolls to reveal the section
	$(".sectiondivider").on('click', function(event) {
        var targetHeight = $(event.target.parentNode).offset().top
        $('html,body').animate({scrollTop: targetHeight - 82}, 400, "linear");
	});

	// Additions by jki & nipa

	// we now have links between artifacts;
	// this prohibits adding a scroll listener to every anchor (as was done before);
	// we now only have a global listener

	// add a global listener that handles link clicks	
	$(document.body).on('click', '.de a', scrollToArtifactForEvent);

	function scrollToArtifactForEvent(event) {
		var href = event.target.getAttribute("href");
		event.preventDefault();
		document.location.hash = href;
		scrollToArtifactWithName(href);
	}

	function scrollToArtifactWithName(name) {
		var settings = window.navScrollerDefaultSettings;
		var $target = $('[name="' + name + '"]');

		if (! $target.is(':empty')) {
			$('html,body').animate(
				{ scrollTop: $target.offset().top - settings.scrollToOffset },
				settings.scrollSpeed, "easeInOutExpo", function () {
					$target.parents('.artifacts').fadeOut().fadeIn();
				});
		}
	}

	if (location.hash) {
		scrollToArtifactWithName(location.hash);
	}

});
