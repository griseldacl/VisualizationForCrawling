/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 *  Created on 29.10.2005
 *  --- CVS Information ---
 *  $Id: ETAComparator.java,v 1.1 2005/10/29 22:39:23 gregork Exp $
 */
package phex.gui.comparator;

import java.util.Comparator;

import phex.common.TransferDataProvider;

public class ETAComparator implements Comparator
{
    public int compare( Object obj1, Object obj2 )
    {
        if ( obj1 == obj2 || obj1.equals(obj2) )
        {
            return 0;
        }
        TransferDataProvider provider1 = (TransferDataProvider)obj1;
        TransferDataProvider provider2 = (TransferDataProvider)obj2;
        
        long diff = calcTimeRemaining(provider2) 
            - calcTimeRemaining(provider1);
        if ( diff == 0 )
        {
            diff = provider2.hashCode() - provider1.hashCode();
        }
        
        if ( diff < 0 )
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
    
    public long calcTimeRemaining( TransferDataProvider provider )
    {
        long transferredSize = provider.getTransferredDataSize();
        long totalTransferSize = provider.getTransferDataSize();
        long transferRate = provider.getLongTermTransferRate();
        
        if ( totalTransferSize == -1 || 
             provider.getDataTransferStatus() != TransferDataProvider.TRANSFER_RUNNING )
        {
            return TransferDataProvider.INFINITY_ETA_INT;
        }
        else
        {
            long timeRemaining;
            if ( transferRate == 0 )
            {
                return TransferDataProvider.INFINITY_ETA_INT;
            }
            else
            {
                timeRemaining = (long)((totalTransferSize - transferredSize) / transferRate);
                timeRemaining = Math.max( 0, timeRemaining );
                return timeRemaining;
            }    
        }
    }
}
