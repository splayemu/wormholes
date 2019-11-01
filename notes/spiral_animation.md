## Rotating Wormhole

Learning to build CSS animations wasn't too hard! I use CSS every now and again and knew that you could do some cool stuff with SVG paths, so I wanted to give it a try in this project.

I also am using garden [1], a clojure css library and fulcro-css [2] which lets you colocate your CSS next to your React rendering code. In reality I am not even using fulcro-css's main property which generates component specific CSS classes. Instead I just like to define the CSS next to the rendering code. Having the CSS generated from Clojure data structures in the same file does it for me.

For an example, you define the CSS in Clojure vectors and maps like this:
```clj
{:css [[:$spiral-wrapper
          {:height "80%"
           :padding "5px"}]
         [:$spiral-wrapper
          [:path {"fill" "none",
                  "fill-rule" "evenodd",
                  "stroke-width" "2px",
                  "stroke-linejoin" "miter",
                  "stroke-opacity" "1",
                  "stroke" "#000000",
                  "stroke-linecap" "butt"
                  }]]
         [:$wormhole
          {:transition "2s cubic-bezier(0, 0, 0, 1)"
           :transform-origin  "48.5% 49%"
           "stroke-dasharray" (str spiral-length " " spiral-length)}
          [:&$unactivated
           {"transform" "scale(0.2)"
            "stroke-dashoffset" (* spiral-length 0.6)}]
          [:&$activated
           {"stroke-dashoffset" 0}]
          [:&$hover
           {"transform" "scale(0.7)"
            "stroke-dashoffset" (* spiral-length 0.4)}]]
         [:$wormhole-rotating-animation
          ^:prefix {:animation "rotate 2s infinite linear"}]
         rotate
         ]}
```

I created a spiral in inkscape (it has a spiral generator). Then wanted to use CSS to animate it.

The two main effects I was trying to do were:
1. Rotate the spiral
2. Grow the spiral out

### Rotating the Spiral

I started by adding a keyframe:
```css
@keyframes rotate {
  from { transform: "rotate(0deg)"; }
  to { transform: "rotate(360deg)"; }
}
```

And it caused the wormhole to rotate out of the SVG canvas. It was rotating around the top left point of the svg canvas. This helpful guide informed me about the CSS property `transform-origin` which changes for SVG elements where the center point is.

Now I updated the keyframes
```css
@keyframes rotate {
  from { 
    transform: "rotate(0deg)"; 
    transform-origin: "48.5% 49%";
  }
  to { 
    transform: "rotate(360deg)"; 
    transform-origin: "48.5% 49%";
  }
}
```

Or using Garden's syntax, you can define the keyframes like this:
```
(require-macros [garden.def :refer [defkeyframes]])

(defkeyframes rotate
  [:from {:transform       "rotate(0deg)"
          :transform-origin "48.5% 49%"}]
  [:to {:transform         "rotate(360deg)"
        :transform-origin  "48.5% 49%"}])
```

Then on the element you can apply the animation using the CSS property `animation`:
```
{:animation "rotate 2s infinite linear"}
```

#### Really Helpful information on rotating SVGs
https://css-tricks.com/transforms-on-svg-elements/

### Causing the Spiral to Grow

I've seen really cool demonstrations of pure CSS animations on SVGs that trace out an SVG image. Since I have a single spiral path, I can cause the spiral to grow or shrink using the same technique.

These two blog posts outline it really well:
https://jakearchibald.com/2013/animated-line-drawing-svg/
https://css-tricks.com/svg-line-animation-works/

### Switching from Animations to Transitions

Defining the animations in keyframes is intuitive, but in an application (or in my case a game) that has many state changes between off, hover, and clicked, it was hard to synchronize the animations. Instead `transition` interpolates over a set time between the current set of properties and the new set of properties defined on the CSS class the element is assigned.

With Garden's syntax we can see all three states with a minimal amount of css:
```clj
[[:$wormhole
  {:transition "2s cubic-bezier(0, 0, 0, 1)"
   :transform-origin  "48.5% 49%"
   "stroke-dasharray" (str spiral-length " " spiral-length)}
  [:&$unactivated
   {"transform" "scale(0.2)"
    "stroke-dashoffset" (* spiral-length 0.6)}]
  [:&$activated
   {"stroke-dashoffset" 0}]
  [:&$hover
   {"transform" "scale(0.7)"
    "stroke-dashoffset" (* spiral-length 0.4)}]]]
```

1 - https://github.com/noprompt/garden
2 - https://github.com/fulcrologic/fulcro-garden-css
