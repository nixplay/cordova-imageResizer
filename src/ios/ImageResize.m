//
//  ImageResize.m
//  ImageResizer PhoneGap / Cordova Plugin
//
//  Created by Raanan Weber on 02.01.12.
// 
//  The software is open source, MIT Licensed.
//  Copyright (c) 2012-2013 webXells GmbH , http://www.webxells.com. All rights reserved.
//
// Using the following Libraries (Big thanks to the developers!)
// Image Scaling : http://iphonedevelopertips.com/graphics/how-to-scale-an-image-using-an-objective-c-category.html . Source is added with respected copyright.
// NSData Base64 : NSData Base64 extension by Dave Winer. http://colloquy.info/project/browser/trunk/NSDataAdditions.h?rev=1576,  Source is added with original copyright.
//

#import "ImageResize.h"
#import "UIImage+Scale.h"
#import "NSData+Base64.h"

@implementation ImageResize

@synthesize callbackID;

- (void) resizeImage:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
        
    CGFloat width = [[options objectForKey:@"width"] floatValue];  
    CGFloat height = [[options objectForKey:@"height"] floatValue];
    NSInteger quality = [[options objectForKey:@"quality"] integerValue];  
    NSString *format =  [options objectForKey:@"format"];
    NSString *resizeType = [options objectForKey:@"resizeType"];
    bool storeImage = [[options objectForKey:@"storeImage"] boolValue];
    NSString *filename = [options objectForKey:@"filename"];
    bool accountForPixelDensity = [[options objectForKey:@"pixelDensity"] boolValue];

    //Load the image
    UIImage * img = [self getImageUsingOptions:options];   

    UIImage *scaledImage = nil;
    float newHeight;
    float newWidth;
    if ([resizeType isEqualToString:@"factorResize"] == YES) {
        newWidth = img.size.width * width;
        newHeight = img.size.height * height;
    } else if ([resizeType isEqualToString:@"widthResize"] == YES) {
        float scaleFactor = width / img.size.width;
        newWidth = width;
        newHeight = img.size.height * scaleFactor;
    } else {
        newWidth = width;
        newHeight = height;
    }

    //Double size for retina if option set to true
    if (accountForPixelDensity && [[UIScreen mainScreen] respondsToSelector:@selector(scale)] && [[UIScreen mainScreen] scale] == 2.0) {
       newWidth = newWidth * 2;
       newHeight = newHeight * 2;
    }

    scaledImage = [img scaleToSize:CGSizeMake(newWidth, newHeight)];
    NSNumber *newWidthObj = [[NSNumber alloc] initWithFloat:newWidth];
    NSNumber *newHeightObj = [[NSNumber alloc] initWithFloat:newHeight];

    CDVPluginResult* pluginResult = nil;
    if (storeImage) {
        bool written = [self saveImage:scaledImage withOptions:options];
        if (written) {
            NSDictionary* result = [NSDictionary dictionaryWithObjects:[NSArray arrayWithObjects:filename, newWidthObj, newHeightObj, nil] forKeys:[NSArray arrayWithObjects: @"filename", @"width", @"height", nil]];
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
        } else {
           pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
    } else {
        NSData* imageDataObject = nil;
        if ([format isEqualToString:@"png"] == YES) {
            imageDataObject = UIImagePNGRepresentation(scaledImage);
        } else {
            imageDataObject = UIImageJPEGRepresentation(scaledImage, (quality/100.f));
        }

        NSString *encodedString = [imageDataObject base64EncodingWithLineLength:0];
        
        NSDictionary* result = [NSDictionary dictionaryWithObjects:[NSArray arrayWithObjects:encodedString, newWidthObj, newHeightObj, nil] forKeys:[NSArray arrayWithObjects: @"imageData", @"width", @"height", nil]];

        if (encodedString != nil) {
            //Call  the Success Javascript function
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
        } else {
            //Call  the Failure Javascript function
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (UIImage*) getImageUsingOptions:(NSDictionary*)options {
    NSString *imageData = [options objectForKey:@"data"];
    NSString *imageDataType = [options objectForKey:@"imageDataType"];
    
    //Load the image
    UIImage * img = nil;
    if([imageDataType isEqualToString:@"base64Image"]==YES) {
        img = [[UIImage alloc] initWithData:[NSData dataWithBase64EncodedString:imageData]];
    } else {
        img = [[UIImage alloc] initWithData:[NSData dataWithContentsOfURL:[NSURL URLWithString:imageData]]];
    }
    return img;
}

- (void) imageSize:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
    
    UIImage * img = [self getImageUsingOptions:options];   
    NSNumber *width = [[NSNumber alloc] initWithInt:img.size.width];
    NSNumber *height = [[NSNumber alloc] initWithInt:img.size.height];
    NSDictionary* dic = [NSDictionary dictionaryWithObjects:[NSArray arrayWithObjects:width,height,nil] forKeys:[NSArray arrayWithObjects: @"width", @"height", nil]];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dic];
   
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) storeImage:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
    UIImage * img = [self getImageUsingOptions:options];
    [self saveImage:img withOptions:options];
}

- (bool) saveImage:(UIImage *)img withOptions:(NSDictionary *) options {
	 NSString *format =  [options objectForKey:@"format"];
    NSString *filename =  [options objectForKey:@"filename"];
    NSString *directory =  [options objectForKey:@"directory"];
    NSInteger quality = [[options objectForKey:@"quality"] integerValue]; 
    bool photoAlbum = [[options objectForKey:@"photoAlbum"] boolValue];
    if (photoAlbum == YES) {
        UIImageWriteToSavedPhotosAlbum(img, self, @selector(imageSavedToPhotosAlbum:didFinishSavingWithError:contextInfo:), nil);
        return true;
    } else {
        NSData* imageDataObject = nil;
        if (![format isEqualToString:@"jpg"]) {
            imageDataObject = UIImageJPEGRepresentation(img, (quality/100.f));
        } else {
            imageDataObject = UIImagePNGRepresentation(img);
        }
        
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *documentsDirectory = [paths objectAtIndex:0];
        NSMutableString* fullFileName = [NSMutableString stringWithString: documentsDirectory];
        
        if (![directory isEqualToString:@""]) {
            NSString *folderPath = [documentsDirectory stringByAppendingPathComponent:directory];
            if (![[NSFileManager defaultManager] fileExistsAtPath:folderPath]) {
                NSError *error = nil;
                [[NSFileManager defaultManager] createDirectoryAtPath:folderPath withIntermediateDirectories:NO attributes:nil error:&error];
            }
            [fullFileName appendString:@"/"];
            [fullFileName appendString:directory];
        }

        [fullFileName appendString:@"/"];
        [fullFileName appendString:filename];
        NSRange r = [filename rangeOfString:format options:NSCaseInsensitiveSearch];
        if (r.location == NSNotFound) {
            [fullFileName appendString:@"."];
            [fullFileName appendString:format];
        }
        NSError *error = nil;
        bool written = [imageDataObject writeToFile:fullFileName options:NSDataWritingAtomic error:&error];
        if (!written) {
        	  NSLog(@"Write returned error: %@", [error localizedDescription]);
        }
        return written;
    }
}

- (void) imageSavedToPhotosAlbum:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo {
    NSString *message;
    NSString *title;
    if (!error) {
        title = NSLocalizedString(@"Image Saved", @"");
        message = NSLocalizedString(@"The image was placed in your photo album.", @"");
    }
    else {
        title = NSLocalizedString(@"Error", @"");
        message = [error description];
    }
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:title 
                                                    message:message
                                                   delegate:nil
                                          cancelButtonTitle:@"OK"
                                          otherButtonTitles:nil];
    [alert show];
}

@end