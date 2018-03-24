#version 420
out vec4 outColor;

uniform float maxIteration;
uniform float maxValue;

uniform mat4 transform;

vec3 hsv2rgb(vec3 hsv) {
    float hh, p, q, t, ff;
    float i;
   	vec3  rgb;

    if(hsv.y <= 0.0) {       
        rgb.x = hsv.z;
        rgb.y = hsv.z;
        rgb.z = hsv.z;
        return rgb;
    }

    hh = hsv.x;

    if (hh >= 360.0) {
    	hh = 0.0;
    }

    hh /= 60.0;
    i = trunc(hh);

    ff = hh - i;
    p = hsv.z * (1.0 - hsv.y);
    q = hsv.z * (1.0 - (hsv.y * ff));
    t = hsv.z * (1.0 - (hsv.y * (1.0 - ff)));

	if (i == 0) {
        rgb.x = hsv.z;
        rgb.y = t;
        rgb.z = p;
    } else if (i == 1) {
        rgb.x = q;
        rgb.y = hsv.z;
        rgb.z = p;
    } else if (i == 2) {
        rgb.x = p;
        rgb.y = hsv.z;
        rgb.z = t;
    } else if (i == 3) {
        rgb.x = p;
        rgb.y = q;
        rgb.z = hsv.z;
    } else if (i == 4) {
        rgb.x = t;
        rgb.y = p;
        rgb.z = hsv.z;
    } else {
        rgb.x = hsv.z;
        rgb.y = p;
        rgb.z = q;
    }
    return rgb;     
}

void main() {
	vec4 point = vec4(
		gl_FragCoord.x, 
		gl_FragCoord.y,
		0.0, 
		1.0
	);
	
	point = transform * point;

	float cx = point.x;
	float cy = point.y;
	
	float zx = 0;
	float zy = 0;
	
	float i = 0;
	for (i = 0; i < maxIteration; i++) {
		float newZx = zx * zx - zy * zy + cx;
		float newZy = 2 * zx * zy + cy;	
		
		if ((newZx * newZx + newZy * newZy) > maxValue) {
			break;	
		} else {
			zx = newZx;
			zy = newZy;
		}
	}
	
	outColor = vec4(hsv2rgb(vec3(i / maxIteration * 360, 1.0, 1.0)), 1.0);
}
